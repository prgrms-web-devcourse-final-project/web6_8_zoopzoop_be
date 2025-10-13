package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    @Query("""
        select distinct t.tagName
        from Tag t
        where t.dataSource.folder.id = :folderId
    """)
    List<String> findDistinctTagNamesByFolderId(@Param("folderId") Integer folderId);

    @Modifying
    @Query("""
      delete from Tag t
      where t.dataSource.id in (
        select d.id
        from DataSource d
        where d.folder.archive.id = (
          select sa.archive.id
          from Space s join s.sharingArchive sa
          where s.id = :spaceId
        )
      )
    """)
    int bulkDeleteTagsBySpaceId(@Param("spaceId") Integer spaceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from Tag t
        where t.dataSource.id in (
            select d.id
            from DataSource d
            join d.folder f
            join f.archive a
            where a.id in (
                select pa.archive.id
                from PersonalArchive pa
                where pa.member.id = :memberId
            )
        )
    """)
    int bulkDeleteTagsByMemberId(@Param("memberId") Integer memberId);
}
