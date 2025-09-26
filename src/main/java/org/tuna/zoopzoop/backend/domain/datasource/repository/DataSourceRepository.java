package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Integer> {
    List<DataSource> findAllByFolder(Folder folder);

    List<DataSource> findAllByIdIn(Collection<Integer> ids);

    // CHANGED: 특정 멤버(개인 아카이브 소유자) 범위에서 id로 조회 (ownership check)
    @Query("""
        select d from DataSource d
        join d.folder f
        join f.archive a
        join PersonalArchive pa on pa.archive = a
        where d.id = :id
          and pa.member.id = :memberId
        """)
    Optional<DataSource> findByIdAndMemberId(@Param("id") Integer id, @Param("memberId") Integer memberId);

    // CHANGED: 여러 id 중에서 해당 member 소유인 id만 반환 (다건 삭제/검증용)
    @Query("""
        select d.id from DataSource d
        join d.folder f
        join f.archive a
        join PersonalArchive pa on pa.archive = a
        where pa.member.id = :memberId
          and d.id in :ids
        """)
    List<Integer> findExistingIdsInMember(@Param("memberId") Integer memberId, @Param("ids") Collection<Integer> ids);

}

