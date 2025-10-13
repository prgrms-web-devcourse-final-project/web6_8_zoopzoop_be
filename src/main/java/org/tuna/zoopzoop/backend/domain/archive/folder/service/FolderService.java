package org.tuna.zoopzoop.backend.domain.archive.folder.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FileSummary;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final DataSourceRepository dataSourceRepository;

    // ===== 생성 =====
    @Transactional
    public FolderResponse createFolder(Archive archive, String folderName) {
        if (archive == null) throw new NoResultException("아카이브가 존재하지 않습니다.");
        if (folderName == null || folderName.trim().isEmpty())
            throw new IllegalArgumentException("폴더 이름은 비어 있을 수 없습니다.");

        final String requested = folderName.trim();
        String unique = generateUniqueFolderName(archive.getId(), requested);

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                Folder folder = new Folder();
                folder.setArchive(archive);
                folder.setName(unique);
                folder.setDefault(false);

                Folder saved = folderRepository.save(folder);
                return new FolderResponse(saved.getName(), saved.getId());
            } catch (DataIntegrityViolationException e) {
                unique = generateUniqueFolderName(archive.getId(), requested);
            }
        }
        throw new IllegalStateException("동시성 충돌로 폴더 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    }

    // ===== 삭제 =====
    @Transactional
    public String deleteFolder(Archive archive, Integer folderId) {
        Folder folder = folderRepository.findByIdAndArchiveId(folderId, archive.getId())
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        if (folder.isDefault())
            throw new IllegalArgumentException("default 폴더는 삭제할 수 없습니다.");

        // 기본 폴더 확보 (같은 archive)
        Folder defaultFolder = folderRepository.findByArchiveIdAndIsDefaultTrue(archive.getId())
                .orElseThrow(() -> new IllegalStateException("default 폴더가 존재하지 않습니다."));

        // 폴더 내 자료 이관 + soft delete(네 정책 유지)
        List<DataSource> dataSources = dataSourceRepository.findAllByFolderId(folderId);
        LocalDate now = LocalDate.now();
        for (DataSource ds : dataSources) {
            ds.setFolder(defaultFolder);
            ds.setActive(false);
            ds.setDeletedAt(now);
        }

        String name = folder.getName();
        folderRepository.delete(folder);
        return name;
    }

    // ===== 이름 변경 =====
    @Transactional
    public String updateFolderName(Archive archive, Integer folderId, String newName) {
        if (newName == null || newName.trim().isEmpty())
            throw new IllegalArgumentException("폴더 이름은 비어 있을 수 없습니다.");

        Folder folder = folderRepository.findByIdAndArchiveId(folderId, archive.getId())
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        if (folder.isDefault())
            throw new IllegalArgumentException("default 폴더는 이름을 변경할 수 없습니다.");

        // 같은 Archive 내 동명 검사 (자기 자신 제외)
        List<String> conflict = folderRepository.existsNameInArchiveExceptSelf(
                archive.getId(), newName.trim(), folder.getId());
        if (!conflict.isEmpty()) {
            throw new IllegalArgumentException("이미 존재하는 폴더명입니다.");
        }

        folder.setName(newName.trim());
        folderRepository.save(folder);
        return folder.getName();
    }

    // ===== 목록 조회 =====
    @Transactional(readOnly = true)
    public List<FolderResponse> getFolders(Archive archive) {
        return folderRepository.findByArchive(archive).stream()
                .map(f -> new FolderResponse(f.getName(), f.getId()))
                .toList();
    }

    // ===== 폴더 내 파일 조회 =====
    @Transactional(readOnly = true)
    public FolderFilesDto getFilesInFolder(Archive archive, Integer folderId) {
        Folder folder = folderRepository.findByIdAndArchiveId(folderId, archive.getId())
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        var files = dataSourceRepository.findAllByFolderAndIsActiveTrue(folder).stream()
                .map(ds -> new FileSummary(
                        ds.getId(),
                        ds.getTitle(),
                        ds.getDataCreatedDate(),
                        ds.getSummary(),
                        ds.getSourceUrl(),
                        ds.getImageUrl(),
                        ds.getTags() == null ? List.of() : ds.getTags().stream().map(Tag::getTagName).toList(),
                        ds.getCategory() == null ? null : ds.getCategory().name()
                ))
                .toList();

        return new FolderFilesDto(folder.getId(), folder.getName(), files);
    }

    // ===== 기본 폴더 ID 조회 (Archive 스코프) =====
    @Transactional(readOnly = true)
    public Integer getDefaultFolderId(Archive archive) {
        return folderRepository.findByArchiveIdAndIsDefaultTrue(archive.getId())
                .orElseThrow(() -> new NoResultException("default 폴더를 찾을 수 없습니다."))
                .getId();
    }

    // ===== 이름 충돌 유틸 =====
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("^(.*?)(?: \\((\\d+)\\))?$");

    private String generateUniqueFolderName(Integer archiveId, String requested) {
        NameParts nameParts = NameParts.split(requested);
        String file = nameParts.base();
        String fileEnd = file + "\uffff";
        List<String> existing = folderRepository.findNamesForConflictCheck(archiveId, file, fileEnd);
        return pickNextAvailable(file, existing);
    }

    private static String pickNextAvailable(String file, List<String> existing) {
        boolean baseUsed = false;
        Set<Integer> used = new HashSet<>();
        Pattern p = Pattern.compile("^" + Pattern.quote(file) + "(?: \\((\\d+)\\))?$");
        for (String s : existing) {
            var m = p.matcher(s);
            if (m.matches()) {
                if (m.group(1) == null) baseUsed = true;
                else used.add(Integer.parseInt(m.group(1)));
            }
        }
        if (!baseUsed) return file;
        for (int k = 1; k <= used.size() + 1; k++) {
            if (!used.contains(k)) return file + " (" + k + ")";
        }
        return file + " (" + (used.size() + 1) + ")";
    }

    private record NameParts(String base, Integer num) {
        static NameParts split(String name) {
            var m = SUFFIX_PATTERN.matcher(name.trim());
            if (m.matches()) {
                String base = m.group(1).trim();
                Integer n = m.group(2) != null ? Integer.valueOf(m.group(2)) : null;
                return new NameParts(base, n);
            }
            return new NameParts(name.trim(), null);
        }
    }
}

