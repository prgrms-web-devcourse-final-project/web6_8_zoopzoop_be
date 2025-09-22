package org.tuna.zoopzoop.backend.domain.archive.folder.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FileSummary;
import org.tuna.zoopzoop.backend.domain.datasource.dto.FolderFilesDto;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final MemberRepository memberRepository;
    private final PersonalArchiveRepository personalArchiveRepository;
    private final FolderRepository folderRepository;
    private final DataSourceRepository dataSourceRepository;

    /**
     * 현재 로그인 사용자의 PersonalArchive에 폴더 생성
     * - 폴더명 중복 시 "(n)" 추가
     * - 동시성 충돌 시(더블 클릭, 브라우저 재전송) 재시도
     */
    @Transactional
    public FolderResponse createFolderForPersonal(Integer currentMemberId, String folderName) {
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new IllegalArgumentException("폴더 이름은 비어 있을 수 없습니다.");
        }

        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다."));

        Archive archive = personalArchiveRepository.findByMemberId(member.getId())
                .map(PersonalArchive::getArchive)
                .orElseThrow(() -> new IllegalStateException("개인 아카이브가 없습니다."));

        final String requested = folderName.trim();

        // 동시성 춛돌시 2번 재시도
        String unique = generateUniqueFolderName(archive.getId(), requested);
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                Folder folder = new Folder();
                folder.setArchive(archive);
                folder.setName(unique);
                folder.setDefault(false);

                Folder saved = folderRepository.save(folder);
                return new FolderResponse( saved.getId(), saved.getName());
            } catch (DataIntegrityViolationException e) {
                unique = generateUniqueFolderName(archive.getId(), requested);
            }
        }
        throw new IllegalStateException("동시성 충돌로 폴더 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    }

    private static final Pattern SUFFIX_PATTERN = Pattern.compile("^(.*?)(?: \\((\\d+)\\))?$");

    /**
     * 기존 file 명과 같지 않은 최솟값의 이름 생성
     * “폴더명”, "폴더명 (1)"→ "폴더명 (2)"
     * "폴더명", "폴더명 (2)" -> "폴더명 (1)"
     */
    private String generateUniqueFolderName(Integer archiveId, String requested) {
        NameParts nameParts = NameParts.split(requested);

        // 중복 폴더명 탐색
        String file = nameParts.base();
        String fileEnd = file + "\uffff";

        List<String> existing = folderRepository.findNamesForConflictCheck(archiveId, file, fileEnd);

        return pickNextAvailable(file, existing);
    }

    /**
     * 이미 존재하는 이름들 중 가장 작은 비어 있는 번호 반환
     */
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
        return file + " (" + (used.size() + 1) + ")"; // fallback
    }


    /**
     * 입력된 폴더명을 (폴더명, 숫자)로 분리하는 유틸 클래스
     * “폴더명” → (”폴더명”, null)
     * “폴더명(3)” → (”폴더명”, 3)
     */
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
