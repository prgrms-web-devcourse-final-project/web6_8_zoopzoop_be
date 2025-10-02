package org.tuna.zoopzoop.backend.domain.archive.folder.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalArchiveFolderService {

    private final PersonalArchiveRepository personalArchiveRepository;
    private final FolderRepository folderRepository;
    private final FolderService folderService;

    @Transactional
    public FolderResponse createFolder(Integer memberId, String folderName) {
        Archive archive = personalArchiveRepository.findByMemberId(memberId)
                .map(PersonalArchive::getArchive)
                .orElseThrow(() -> new NoResultException("개인 아카이브가 없습니다."));
        return folderService.createFolder(archive, folderName);
    }

    @Transactional
    public String deleteFolder(Integer memberId, Integer folderId) {
        // 개인 전용 “소유 확인” 쿼리로 빠르게 가드
        Folder folder = folderRepository.findByIdAndMemberId(folderId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));
        return folderService.deleteFolder(folder.getArchive(), folderId);
    }

    @Transactional
    public String updateFolderName(Integer memberId, Integer folderId, String newName) {
        Folder folder = folderRepository.findByIdAndMemberId(folderId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));
        return folderService.updateFolderName(folder.getArchive(), folderId, newName);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFolders(Integer memberId) {
        Archive archive = personalArchiveRepository.findByMemberId(memberId)
                .map(PersonalArchive::getArchive)
                .orElseThrow(() -> new NoResultException("개인 아카이브가 존재하지 않습니다."));
        return folderService.getFolders(archive);
    }

    @Transactional(readOnly = true)
    public FolderFilesDto getFilesInFolder(Integer memberId, Integer folderId) {
        Folder folder = folderRepository.findByIdAndMemberId(folderId, memberId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));
        return folderService.getFilesInFolder(folder.getArchive(), folderId);
    }

    @Transactional(readOnly = true)
    public Integer getDefaultFolderId(Integer memberId) {
        Folder folder = folderRepository.findDefaultFolderByMemberId(memberId)
                .orElseThrow(() -> new NoResultException("default 폴더를 찾을 수 없습니다."));
        return folder.getId();
    }
}
