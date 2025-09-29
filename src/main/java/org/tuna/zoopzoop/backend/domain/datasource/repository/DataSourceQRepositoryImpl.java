// src/main/java/.../domain/datasource/repository/DataSourceQRepositoryImpl.java
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
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.QDataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.QTag;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DataSourceQRepositoryImpl implements DataSourceQRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<DataSourceSearchItem> search(Integer memberId, DataSourceSearchCondition cond, Pageable pageable) {
        QDataSource ds = QDataSource.dataSource;
        QFolder folder = QFolder.folder;
        QPersonalArchive pa = QPersonalArchive.personalArchive;
        QTag tag = QTag.tag;

        // ===== where 절 구성 =====
        BooleanBuilder where = new BooleanBuilder()
                .and(ds.isActive.isTrue()); // 활성 자료만

        if (cond.getTitle() != null && !cond.getTitle().isBlank()) {
            where.and(ds.title.containsIgnoreCase(cond.getTitle()));
        }
        if (cond.getSummary() != null && !cond.getSummary().isBlank()) {
            where.and(ds.summary.containsIgnoreCase(cond.getSummary()));
        }
        if (cond.getCreatedAtAfter() != null) { // 원본 작성일(LocalDate) 기준
            LocalDate from = cond.getCreatedAtAfter();
            where.and(ds.dataCreatedDate.goe(from));
        }
        if (cond.getFolderName() != null && !cond.getFolderName().isBlank()) {
            where.and(ds.folder.name.eq(cond.getFolderName()));
        }
        if (cond.getCategory() != null && !cond.getCategory().isBlank()) {
            try {
                where.and(ds.category.eq(Category.valueOf(cond.getCategory().toUpperCase())));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("잘못된 category 값입니다: " + cond.getCategory());
            }
        }

        // 소유권 제한: 해당 멤버의 아카이브 범위
        BooleanBuilder ownership = new BooleanBuilder()
                .and(pa.member.id.eq(memberId));

        // ===== count 쿼리 =====
        JPAQuery<Long> countQuery = queryFactory
                .select(ds.id.countDistinct())
                .from(ds)
                .join(ds.folder, folder)
                .join(pa).on(pa.archive.eq(folder.archive))
                .where(where.and(ownership));

        // ===== content 쿼리 =====
        JPAQuery<Tuple> contentQuery = queryFactory
                .select(ds.id, ds.title, ds.dataCreatedDate, ds.summary, ds.sourceUrl, ds.imageUrl, ds.category)
                .from(ds)
                .join(ds.folder, folder)
                .join(pa).on(pa.archive.eq(folder.archive))
                .where(where.and(ownership));

        // 정렬 적용
        List<OrderSpecifier<?>> orderSpecifiers = toOrderSpecifiers(pageable.getSort());
        if (!orderSpecifiers.isEmpty()) {
            contentQuery.orderBy(orderSpecifiers.toArray(new OrderSpecifier<?>[0]));
        } else {
            contentQuery.orderBy(ds.dataCreatedDate.desc()); // 기본 최신순
        }

        // ===== 실행: 페이지 데이터 =====
        List<Tuple> tuples = contentQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 개수
        Long totalCount = countQuery.fetchOne();
        long total = (totalCount == null ? 0L : totalCount);

        // ===== 태그 배치 조회 (응답용) =====
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

        // ===== Tuple -> DTO (dataCreatedDate는 LocalDate → atStartOfDay로 LocalDateTime 변환) =====
        List<DataSourceSearchItem> content = tuples.stream()
                .map(row -> new DataSourceSearchItem(
                        row.get(ds.id),
                        row.get(ds.title),
                        row.get(ds.dataCreatedDate),
                        row.get(ds.summary),
                        row.get(ds.sourceUrl),
                        row.get(ds.imageUrl),
                        tagsById.getOrDefault(row.get(ds.id), List.of()),
                        Objects.requireNonNull(row.get(ds.category)).name()
                ))
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    // Q타입 의존 없이 타입별 Path 지정
    private List<OrderSpecifier<?>> toOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isEmpty()) return List.of();

        // QDataSource.dataSource 의 alias가 "dataSource" 이므로 동일하게 맞춘다.
        PathBuilder<DataSource> root =
                new PathBuilder<>(DataSource.class, "dataSource");

        List<OrderSpecifier<?>> specs = new ArrayList<>();
        for (Sort.Order o : sort) {
            Order dir = o.isAscending() ? Order.ASC : Order.DESC;
            switch (o.getProperty()) {
                case "title" -> specs.add(new OrderSpecifier<>(dir, root.getString("title")));
                case "createdAt" -> specs.add(new OrderSpecifier<>(dir, root.getDateTime("createdAt", java.time.LocalDateTime.class)));
                case "dataCreatedDate" -> specs.add(new OrderSpecifier<>(dir, root.getDate("dataCreatedDate", java.time.LocalDate.class)));
                default -> { /* 화이트리스트 외 필드는 무시 */ }
            }
        }
        return specs;
    }
}
