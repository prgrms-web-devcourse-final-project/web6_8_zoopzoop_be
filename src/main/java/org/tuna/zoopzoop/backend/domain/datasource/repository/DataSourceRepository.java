package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Integer> {
    List<DataSource> findAllByFolder(Folder folder);

    List<DataSource> findAllByIdIn(Collection<Integer> ids);

    // 개인 아카이브 범위에서 id로 조회 (ownership check)
    @Query("""
        select d from DataSource d
        join d.folder f
        join f.archive a
        join PersonalArchive pa on pa.archive = a
        where d.id = :id
          and pa.member.id = :memberId
    """)
    Optional<DataSource> findByIdAndMemberId(@Param("id") Integer id, @Param("memberId") Integer memberId);

    // 여러 id 중에서 해당 member 소유인 id만 반환 (다건 삭제/검증용)
    @Query("""
        select d.id from DataSource d
        join d.folder f
        join f.archive a
        join PersonalArchive pa on pa.archive = a
        where pa.member.id = :memberId
          and d.id in :ids
    """)
    List<Integer> findExistingIdsInMember(@Param("memberId") Integer memberId, @Param("ids") Collection<Integer> ids);

    Optional<DataSource> findByFolderIdAndTitle(Integer folderId, String title);

    List<DataSource> findAllByFolderId(Integer folderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DataSource d set d.isActive=false, d.deletedAt=:ts where d.id in :ids")
    int softDeleteAllByIds(@Param("ids") List<Integer> ids, @Param("ts") LocalDateTime ts);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DataSource d set d.isActive=true, d.deletedAt=null where d.id in :ids")
    int restoreAllByIds(@Param("ids") List<Integer> ids);

    @Query("""
    select d from DataSource d
    join d.folder f
    where d.id = :id and f.archive.id = :archiveId
""")
    Optional<DataSource> findByIdAndArchiveId(@Param("id") Integer id, @Param("archiveId") Integer archiveId);

    @Query("""
    select d.id from DataSource d
    join d.folder f
    where f.archive.id = :archiveId and d.id in :ids
""")
    List<Integer> findExistingIdsInArchive(@Param("archiveId") Integer archiveId, @Param("ids") Collection<Integer> ids);

}

