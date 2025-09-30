package org.tuna.zoopzoop.backend.domain.datasource.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.QPersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.QFolder;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.QDataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.QTag;

import java.util.*;
import java.util.stream.Collectors;

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

        // where
        BooleanBuilder where = new BooleanBuilder();

        if (cond.getIsActive() == null || Boolean.TRUE.equals(cond.getIsActive())) {
            where.and(ds.isActive.isTrue());
        } else {
            where.and(ds.isActive.isFalse());
        }
        if (cond.getTitle() != null && !cond.getTitle().isBlank()) {
            where.and(ds.title.containsIgnoreCase(cond.getTitle()));
        }
        if (cond.getSummary() != null && !cond.getSummary().isBlank()) {
            where.and(ds.summary.containsIgnoreCase(cond.getSummary()));
        }
        if (cond.getCategory() != null && !cond.getCategory().isBlank()) {
            where.and(ds.category.stringValue().containsIgnoreCase(cond.getCategory()));
        }
        if (cond.getFolderName() != null && !cond.getFolderName().isBlank()) {
            where.and(ds.folder.name.eq(cond.getFolderName()));
        }

        BooleanBuilder ownership = new BooleanBuilder()
                .and(pa.member.id.eq(memberId));

        // count
        JPAQuery<Long> countQuery = queryFactory
                .select(ds.id.countDistinct())
                .from(ds)
                .join(ds.folder, folder)
                .join(pa).on(pa.archive.eq(folder.archive))
                .where(where.and(ownership));

        // content
        JPAQuery<Tuple> contentQuery = queryFactory
                .select(ds.id, ds.title, ds.dataCreatedDate, ds.summary, ds.sourceUrl, ds.imageUrl, ds.category)
                .from(ds)
                .join(ds.folder, folder)
                .join(pa).on(pa.archive.eq(folder.archive))
                .where(where.and(ownership));

        List<OrderSpecifier<?>> orderSpecifiers = toOrderSpecifiers(pageable.getSort());
        if (!orderSpecifiers.isEmpty()) {
            contentQuery.orderBy(orderSpecifiers.toArray(new OrderSpecifier<?>[0]));
        } else {
            contentQuery.orderBy(ds.dataCreatedDate.desc());
        }

        // fetch
        List<Tuple> tuples = contentQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

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

        // map to DTO
        List<DataSourceSearchItem> content = tuples.stream()
                .map(row -> new DataSourceSearchItem(
                        row.get(ds.id),
                        row.get(ds.title),
                        row.get(ds.dataCreatedDate), // LocalDate 그대로 내려줌
                        row.get(ds.summary),
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
                case "title" ->
                        specs.add(new OrderSpecifier<>(dir, root.getString("title")));
                case "createdAt" -> // 요청 키
                        specs.add(new OrderSpecifier<>(dir, root.getDate("dataCreatedDate", java.time.LocalDate.class)));
                default -> { }
            }
        }
        return specs;
    }
}
