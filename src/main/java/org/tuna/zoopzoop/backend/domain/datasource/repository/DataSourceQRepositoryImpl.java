package org.tuna.zoopzoop.backend.domain.datasource.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.QPersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.QFolder;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.QDataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.QTag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Repository
@RequiredArgsConstructor
public class DataSourceQRepositoryImpl implements DataSourceQRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<DataSourceSearchItem> search(Integer memberId, DataSourceSearchCondition cond, Pageable pageable) {
        if (memberId == null)
            throw new IllegalArgumentException("memberId must not be null");

        QDataSource ds = QDataSource.dataSource;
        QFolder folder = QFolder.folder;
        QPersonalArchive pa = QPersonalArchive.personalArchive;
        QTag tag = QTag.tag;

        BooleanBuilder where = new BooleanBuilder();

        if (cond.getIsActive() == null || Boolean.TRUE.equals(cond.getIsActive())) where.and(ds.isActive.isTrue());
        else where.and(ds.isActive.isFalse());

        if (hasText(cond.getTitle())) where.and(ds.title.containsIgnoreCase(cond.getTitle()));
        if (hasText(cond.getSummary())) where.and(ds.summary.containsIgnoreCase(cond.getSummary()));
        if (hasText(cond.getCategory())) where.and(ds.category.stringValue().containsIgnoreCase(cond.getCategory()));
        if (hasText(cond.getSource())) where.and(ds.source.containsIgnoreCase(cond.getSource()));

        if (hasText(cond.getKeyword())) {
            String kw = cond.getKeyword();
            where.and(
                    ds.title.containsIgnoreCase(kw)
                            .or(ds.summary.containsIgnoreCase(kw))
                            .or(ds.source.containsIgnoreCase(kw))
                            .or(ds.category.stringValue().containsIgnoreCase(kw))
            );
        }

        if (hasText(cond.getFolderName())) where.and(ds.folder.name.eq(cond.getFolderName()));
        if (cond.getFolderId() != null) where.and(ds.folder.id.eq(cond.getFolderId()));

        BooleanBuilder ownership = new BooleanBuilder().and(pa.member.id.eq(memberId));

        // count
        JPAQuery<Long> countQuery = queryFactory
                .select(ds.id.countDistinct())
                .from(ds)
                .join(ds.folder, folder)
                .join(pa).on(pa.archive.eq(folder.archive))
                .where(where.and(ownership));

        // content
        JPAQuery<Tuple> contentQuery = queryFactory
                .select(ds.id, ds.title, ds.dataCreatedDate, ds.summary, ds.source, ds.sourceUrl, ds.imageUrl, ds.category)
                .from(ds)
                .join(ds.folder, folder)
                .join(pa).on(pa.archive.eq(folder.archive))
                .where(where.and(ownership));

        List<OrderSpecifier<?>> orderSpecifiers = toOrderSpecifiers(pageable.getSort());
        if (!orderSpecifiers.isEmpty()) contentQuery.orderBy(orderSpecifiers.toArray(new OrderSpecifier<?>[0]));
        else contentQuery.orderBy(ds.createDate.desc());

        List<Tuple> tuples = contentQuery.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long totalCount = countQuery.fetchOne();
        long total = (totalCount == null ? 0L : totalCount);

        // 태그 배치 조회
        List<Integer> ids = tuples.stream().map(t -> t.get(ds.id)).toList();

        Map<Integer, List<String>> tagsById = ids.isEmpty() ? Map.of()
                : queryFactory
                .select(ds.id, tag.tagName)
                .from(ds)
                .leftJoin(ds.tags, tag)
                .where(ds.id.in(ids))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(ds.id),
                        Collectors.mapping(row -> row.get(tag.tagName), Collectors.toList())
                ));

        List<DataSourceSearchItem> content = tuples.stream()
                .map(row -> new DataSourceSearchItem(
                        row.get(ds.id),
                        row.get(ds.title),
                        row.get(ds.dataCreatedDate),
                        row.get(ds.summary),
                        row.get(ds.source),
                        row.get(ds.sourceUrl),
                        row.get(ds.imageUrl),
                        tagsById.getOrDefault(row.get(ds.id), List.of()),
                        row.get(ds.category).name()
                ))
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    // createdAt / title 허용. createdAt은 내부적으로 dataCreatedDate로 매핑
    private List<OrderSpecifier<?>> toOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isEmpty()) return List.of();

        PathBuilder<org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource> root =
                new PathBuilder<>(org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource.class, "dataSource");

        List<OrderSpecifier<?>> specs = new ArrayList<>();
        for (Sort.Order o : sort) {
            Order dir = o.isAscending() ? Order.ASC : Order.DESC;
            switch (o.getProperty()) {
                case "title" -> specs.add(new OrderSpecifier<>(dir, root.getString("title")));
                case "createdAt" -> specs.add(
                        new OrderSpecifier<>(dir, root.getDate("dataCreatedDate", LocalDate.class))
                );
                default -> { /* 무시 */ }
            }
        }
        return specs;
    }

    @Override
    public Page<DataSourceSearchItem> searchInArchive(Integer archiveId, DataSourceSearchCondition cond, Pageable pageable) {
        if (archiveId == null) throw new IllegalArgumentException("archiveId must not be null");

        QDataSource ds = QDataSource.dataSource;
        QFolder folder = QFolder.folder;
        QTag tag = QTag.tag;

        BooleanBuilder where = new BooleanBuilder();
        if (cond.getIsActive() == null || Boolean.TRUE.equals(cond.getIsActive())) where.and(ds.isActive.isTrue());
        else where.and(ds.isActive.isFalse());

        if (hasText(cond.getTitle())) where.and(ds.title.containsIgnoreCase(cond.getTitle()));
        if (hasText(cond.getSummary())) where.and(ds.summary.containsIgnoreCase(cond.getSummary()));
        if (hasText(cond.getCategory())) where.and(ds.category.stringValue().containsIgnoreCase(cond.getCategory()));
        if (hasText(cond.getSource())) where.and(ds.source.containsIgnoreCase(cond.getSource()));
        if (hasText(cond.getKeyword())) {
            String kw = cond.getKeyword();
            where.and(
                    ds.title.containsIgnoreCase(kw)
                            .or(ds.summary.containsIgnoreCase(kw))
                            .or(ds.source.containsIgnoreCase(kw))
                            .or(ds.category.stringValue().containsIgnoreCase(kw))
            );
        }
        if (hasText(cond.getFolderName())) where.and(ds.folder.name.eq(cond.getFolderName()));
        if (cond.getFolderId() != null) where.and(ds.folder.id.eq(cond.getFolderId()));

        BooleanBuilder scope = new BooleanBuilder().and(folder.archive.id.eq(archiveId));

        JPAQuery<Long> countQuery = queryFactory
                .select(ds.id.countDistinct())
                .from(ds)
                .join(ds.folder, folder)
                .where(where.and(scope));

        JPAQuery<Tuple> contentQuery = queryFactory
                .select(ds.id, ds.title, ds.dataCreatedDate, ds.summary, ds.source, ds.sourceUrl, ds.imageUrl, ds.category)
                .from(ds)
                .join(ds.folder, folder)
                .where(where.and(scope));

        List<OrderSpecifier<?>> orderSpecifiers = toOrderSpecifiers(pageable.getSort());
        if (!orderSpecifiers.isEmpty()) contentQuery.orderBy(orderSpecifiers.toArray(new OrderSpecifier<?>[0]));
        else contentQuery.orderBy(ds.createDate.desc());

        List<Tuple> tuples = contentQuery.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long totalCount = countQuery.fetchOne();
        long total = (totalCount == null ? 0L : totalCount);

        Map<Integer, List<String>> tagsById = tuples.isEmpty() ? Map.of()
                : queryFactory
                .select(ds.id, tag.tagName)
                .from(ds)
                .leftJoin(ds.tags, tag)
                .where(ds.id.in(tuples.stream().map(t -> t.get(ds.id)).toList()))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(ds.id),
                        Collectors.mapping(row -> row.get(tag.tagName), Collectors.toList())
                ));

        List<DataSourceSearchItem> content = tuples.stream()
                .map(row -> new DataSourceSearchItem(
                        row.get(ds.id),
                        row.get(ds.title),
                        row.get(ds.dataCreatedDate),
                        row.get(ds.summary),
                        row.get(ds.source),
                        row.get(ds.sourceUrl),
                        row.get(ds.imageUrl),
                        tagsById.getOrDefault(row.get(ds.id), List.of()),
                        row.get(ds.category).name()
                ))
                .toList();

        return new PageImpl<>(content, pageable, total);
    }
}
