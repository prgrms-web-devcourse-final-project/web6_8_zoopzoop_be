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
}
