package org.tuna.zoopzoop.backend.domain.archive.folder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Integer>{
    /**
     * 폴더 중복명 검사
     * @param archiveId   아카이브 Id
     * @param filename    "파일명"
     * @param filenameEnd "파일명 + \ufffff"
     */
    @Query("""
        select f.name
        from Folder f
        where f.archive.id = :archiveId
          and f.name >= :filename
          and f.name < :filenameEnd
    """)
    List<String> findNamesForConflictCheck(Integer archiveId, String filename, String filenameEnd);

    List<Folder> findByArchive(Archive archive);

    Optional<Folder> findByIdAndArchive(Integer id, Archive archive);
}
