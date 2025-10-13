package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DataSourceRepository extends JpaRepository<DataSource, Integer> {

    // 개인 소유 검증: DataSource -> Folder -> Archive -> PersonalArchive.member.id
    @Query("""
           select ds
           from DataSource ds
             join ds.folder f
             join f.archive a
             join PersonalArchive pa
               on pa.archive = a
           where ds.id = :dataSourceId and pa.member.id = :memberId
           """)
    Optional<DataSource> findByIdAndMemberId(@Param("dataSourceId") int dataSourceId,
                                             @Param("memberId") int memberId);

    // 공유 스코프 검증: DataSource -> Folder -> Archive.id
    @Query("""
           select ds
           from DataSource ds
             join ds.folder f
           where ds.id = :dataSourceId and f.archive.id = :archiveId
           """)
    Optional<DataSource> findByIdAndArchiveId(@Param("dataSourceId") int dataSourceId,
                                              @Param("archiveId") int archiveId);

    // 존재/소유 검증용: 요청 ids 중 member가 가진 것만 id 리스트로 반환
    @Query("""
           select ds.id
           from DataSource ds
             join ds.folder f
             join f.archive a
             join PersonalArchive pa
               on pa.archive = a
           where pa.member.id = :memberId and ds.id in :ids
           """)
    List<Integer> findExistingIdsInMember(@Param("memberId") int memberId,
                                          @Param("ids") Collection<Integer> ids);

    // 존재/스코프 검증용: 요청 ids 중 archive에 속한 것만 id 리스트로 반환
    @Query("""
           select ds.id
           from DataSource ds
             join ds.folder f
           where f.archive.id = :archiveId and ds.id in :ids
           """)
    List<Integer> findExistingIdsInArchive(@Param("archiveId") int archiveId,
                                           @Param("ids") Collection<Integer> ids);

    // 멤버 탈퇴/스페이스 삭제 등에서 아카이브 단위 하위 자료 일괄 물리삭제
    @Transactional
    @Modifying
    @Query("""
           delete from DataSource ds
           where ds.folder.id in (
             select f.id from Folder f
             where f.archive.id = :archiveId
           )
           """)
    void deleteByArchiveId(@Param("archiveId") int archiveId);

    List<DataSource> findAllByFolderAndIsActiveTrue(Folder folder);

    List<DataSource> findAllByFolderId(Integer folderId);


    Optional<DataSource> findByFolderIdAndTitle(Integer folderId, String title);

    @Modifying
    @Query("""
      delete from DataSource d
      where d.folder.archive.id = (
        select sa.archive.id
        from Space s join s.sharingArchive sa
        where s.id = :spaceId
      )
    """)
    int bulkDeleteBySpaceId(@Param("spaceId") Integer spaceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from DataSource d
        where d.folder.archive.id in (
            select pa.archive.id
            from PersonalArchive pa
            where pa.member.id = :memberId
        )
    """)
    int bulkDeleteByMemberId(@Param("memberId") Integer memberId);
}
