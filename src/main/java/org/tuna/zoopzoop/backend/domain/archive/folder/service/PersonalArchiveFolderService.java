package org.tuna.zoopzoop.backend.domain.archive.folder.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;

import java.util.List;

// domain.archive.folder.service.PersonalArchiveFolderService.java (신규)
@Service
@RequiredArgsConstructor
public class PersonalArchiveFolderService {
    private final PersonalArchiveRepository personalArchiveRepository;
    private final FolderService folderService;

    private Archive getArchive(Integer memberId) {
        return personalArchiveRepository.findByMemberId(memberId)
                .map(PersonalArchive::getArchive)
                .orElseThrow(() -> new NoResultException("개인 아카이브가 없습니다."));
    }

    @Transactional
    public FolderResponse createFolder(Integer memberId, String folderName) {
        return folderService.createFolder(getArchive(memberId), folderName);
    }

    @Transactional
    public String deleteFolder(Integer memberId, Integer folderId) {
        return folderService.deleteFolder(getArchive(memberId), folderId);
    }

    @Transactional
    public String updateFolderName(Integer memberId, Integer folderId, String newName) {
        return folderService.updateFolderName(getArchive(memberId), folderId, newName);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listFolders(Integer memberId) {
        return folderService.listFolders(getArchive(memberId));
    }

    @Transactional(readOnly = true)
    public FolderFilesDto getFilesInFolder(Integer memberId, Integer folderId) {
        return folderService.getFilesInFolder(getArchive(memberId), folderId);
    }

    @Transactional(readOnly = true)
    public Integer getDefaultFolderId(Integer memberId) {
        return folderService.getDefaultFolderId(getArchive(memberId));
    }
}

