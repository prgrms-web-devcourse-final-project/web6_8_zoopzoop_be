package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataSourceService {
    private final DataSourceRepository dataSourceRepository;
    private final FolderRepository folderRepository;
    private final PersonalArchiveRepository personalArchiveRepository;

    /**
     * 지정한 folder 위치에 자료 생성
     * @param currentMemberId  현재 로그인한 유저 Id
     * @param sourceUrl        생성할 자료의 url
     * @param folderId         생성될 폴더 위치 Id
     */
    @Transactional
    public int createDataSource(int currentMemberId, String sourceUrl, Integer folderId) {
        Folder folder;
        // default 폴더에 데이터 넣을 경우
        if(folderId == null)
            folder = findDefaultFolder(currentMemberId);
            // Id에 해당하는 폴더에 데이터 넣을 경우
        else
            folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        // 임시 파일 생성 메서드
        DataSource ds = buildDataSource(sourceUrl, folder);
        DataSource saved = dataSourceRepository.save(ds);

        return saved.getId();
    }

    /**
     * 임시 data build 메서드
     * 추후 title,summary, tag, category, imgUrl 불러올 예정
     */
    private DataSource buildDataSource(String sourceUrl, Folder folder) {
        DataSource ds = new DataSource();
        ds.setFolder(folder);
        ds.setSourceUrl(sourceUrl);
        ds.setTitle("자료 제목");
        ds.setSources("www.examplesource.com");
        ds.setSummary("설명");
        ds.setImageUrl("www.example.com/img");
        ds.setDataCreatedDate(LocalDate.now());
        ds.setActive(true);
        return ds;
    }

    /**
     *  default 폴더에 해당하는 FolderId 반환
     *  folder의 isDefault 속성 + 인덱스(archiveId)로 탐색
     */
    private Folder findDefaultFolder(int currentMemberId) {
        // 현재 로그인 Id 기반 Personal Archive Id 탐색
        PersonalArchive pa = personalArchiveRepository.findByMemberId(currentMemberId)
                .orElseThrow(() -> new NoResultException("개인 아카이브를 찾을 수 없습니다."));

        // 2. PersonalArchive 안에 연결된 Archive 조회
        Integer archiveId = pa.getArchive().getId();

        // 3. 해당 Archive 내 default 폴더 조회
        return folderRepository.findByArchiveIdAndIsDefaultTrue(archiveId)
                .orElseThrow(() -> new NoResultException("default 폴더를 찾을 수 없습니다."));
    }

    /**
     * 자료 단건 삭제
     * soft delete 추후 구현 예정
     * @param dataSourceId 삭제할 자료 Id
     */
    @Transactional
    public int deleteById(Integer dataSourceId) {
        DataSource ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        /* 추후 권한 체크 예외 필요 */

        dataSourceRepository.delete(ds);
        return dataSourceId;
    }

    /**
     * 자료 다건 삭제
     * 모든 자료 id가 존재해야 함 (부분 존재 시 404)
     */
    @Transactional
    public void deleteMany(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 자료 id 배열이 비어있습니다.");
        }

        // 존재 여부 검증 (부분 존재 시 누락 ID 명시)
        List<Integer> existing = dataSourceRepository.findExistingIds(ids);
        if (existing.size() != ids.size()) {
            Set<Integer> missing = new HashSet<>(ids);
            missing.removeAll(new HashSet<>(existing));
            throw new NoResultException("존재하지 않는 자료 ID 포함: " + missing);
        }

        dataSourceRepository.deleteAllByIdInBatch(ids);
    }

    /**
     * 자료 위치 단건 이동
     */
    @Transactional
    public MoveResult moveDataSource(Integer currentMemberId, Integer dataSourceId, Integer targetFolderId) {

        // 자료 확인
        DataSource ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        Folder targetFolder = resolveTargetFolder(currentMemberId, targetFolderId);

        // 동일 폴더로 이동 요청 -> 통과
        if (ds.getFolder().getId() == targetFolder.getId())
            return  new MoveResult(ds.getId(), targetFolder.getId());

        // 목적지 폴더 내 파일명 중복 확인
//        if (dataSourceRepository.existsByFolder_IdAndTitle(targetFolderId, ds.getTitle()))
//            throw new IllegalStateException("해당 폴더에 동일한 제목의 자료가 이미 존재합니다.");

        ds.setFolder(targetFolder);

        return  new MoveResult(ds.getId(), targetFolder.getId());
    }



    /**
     * 자료 위치 다건 이동
     */
    @Transactional
    public void moveDataSources(Integer currentMemberId, Integer targetFolderId, List<Integer> dataSourceIds) {
        // 1) 요소 null 검증 (서비스 방어)
        if (dataSourceIds.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("자료 id 목록에 null이 포함되어 있습니다.");

        // 자료 Id 중복 확인
        Map<Integer, Long> counts = dataSourceIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        List<Integer> duplicates = counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("같은 자료를 두 번 선택했습니다: " + duplicates);
        }

        // 목적지 폴더 확인
        Folder targetFolder = resolveTargetFolder(currentMemberId, targetFolderId);

        // 목록의 각 자료 확인
        List<DataSource> list = dataSourceRepository.findAllByIdIn(dataSourceIds);
        if (list.size() != dataSourceIds.size())
            throw new NoResultException("요청한 자료 중 존재하지 않는 항목이 있습니다.");

        // 목적지 폴더 추출
        List<DataSource> needMove = list.stream()
                .filter(ds -> !Objects.equals(ds.getFolder().getId(), targetFolder.getId()))
                .toList();

        // 이미 모두 이동한 경우
        if (needMove.isEmpty())
            return;

        // 같은 이름의 자료 여러 개 이동 시 충돌
        /*
        Map<String, Long> reqTitleCount = needMove.stream()
                .collect(Collectors.groupingBy(DataSource::getTitle, Collectors.counting()));
        List<String> internalDup = reqTitleCount.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!internalDup.isEmpty()) {
            throw new IllegalStateException("요청 목록 내부에 중복 제목이 포함되어 있습니다: " + internalDup);
        }

         이동할 폴더에 이미 같은 제목이 존재하는지 확인
        List<String> titles = needMove.stream().map(DataSource::getTitle).toList();
        List<String> conflicts = titles.isEmpty()
                ? List.of()
                : dataSourceRepository.findExistingTitlesInFolder(targetFolderId, titles);

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("대상 폴더에 이미 존재하는 제목이 있어 이동할 수 없습니다: " + conflicts);
        }
        */
        needMove.forEach(ds -> ds.setFolder(targetFolder));
    }

    // 대상 폴더 해석
    private Folder resolveTargetFolder(Integer currentMemberId, Integer targetFolderId) {
        if (targetFolderId == null) {
            return folderRepository.findDefaultFolderByMemberId(currentMemberId)
                    .orElseThrow(() -> new NoResultException("기본 폴더가 존재하지 않습니다."));
        }
        return folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));
    }

    public Integer updateDataSource(Integer dataSourceId, String newTitle, String newSummary) {
        DataSource ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        if (newTitle != null && !newTitle.isBlank())
            ds.setTitle(newTitle);

        if (newSummary != null && !newSummary.isBlank())
            ds.setSummary(newSummary);

        return ds.getId();
    }

    public record MoveResult(Integer datasourceId, Integer folderId) {}
}
