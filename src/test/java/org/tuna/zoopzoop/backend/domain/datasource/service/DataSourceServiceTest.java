package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock private DataSourceRepository dataSourceRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private PersonalArchiveRepository personalArchiveRepository;

    @InjectMocks private DataSourceService dataSourceService;

    // create
    @Test
    @DisplayName("폴더 생성 성공- folderId=null 이면 default 폴더에 자료 생성")
    void createDataSource_defaultFolder() {
        int currentMemberId = 10;
        String sourceUrl = "https://example.com/a";

        // PersonalArchive 생성 시 Archive + default folder 자동 생성됨
        Member member = new Member("u1", "k-1", Provider.KAKAO, null);
        PersonalArchive pa = new PersonalArchive(member);
        Integer archiveId = pa.getArchive().getId(); // 실제 id는 없지만, 아래 anyInt()로 받게 스텁함

        when(personalArchiveRepository.findByMemberId(eq(currentMemberId)))
                .thenReturn(Optional.of(pa));

        Folder defaultFolder = new Folder("default");
        // 리얼 구현은 archiveId 기준으로 찾으니 시그니처 맞추기
        when(folderRepository.findByArchiveIdAndIsDefaultTrue(anyInt()))
                .thenReturn(Optional.of(defaultFolder));

        when(dataSourceRepository.save(any(DataSource.class)))
                .thenAnswer(inv -> {
                    DataSource ds = inv.getArgument(0);
                    ReflectionTestUtils.setField(ds, "id", 123);
                    return ds;
                });

        int id = dataSourceService.createDataSource(currentMemberId, sourceUrl, null);
        assertThat(id).isEqualTo(123);
    }


    @Test
    @DisplayName("폴더 생성 성공- folderId가 주어지면 해당 폴더에 자료 생성")
    void createDataSource_specificFolder() {
        // given
        int currentMemberId = 10;
        String sourceUrl = "https://example.com/b";
        Integer folderId = 77;

        Folder target = new Folder("target");
        // BaseEntity.id 는 protected setter → 리플렉션으로 주입
        org.springframework.test.util.ReflectionTestUtils.setField(target, "id", folderId);

        when(folderRepository.findById(eq(folderId))).thenReturn(Optional.of(target));

        // save(...) 시에 PK가 채워진 것처럼 반환
        when(dataSourceRepository.save(any(DataSource.class)))
                .thenAnswer(inv -> {
                    DataSource ds = inv.getArgument(0);
                    org.springframework.test.util.ReflectionTestUtils.setField(ds, "id", 456);
                    return ds;
                });

        // when
        int id = dataSourceService.createDataSource(currentMemberId, sourceUrl, folderId);

        // then
        assertThat(id).isEqualTo(456);
    }

    @Test
    @DisplayName("폴대 생성 실패 - folderId가 주어졌는데 대상 폴더가 없으면 예외")
    void createDataSource_folderNotFound() {
        // given
        Integer folderId = 999;

        when(folderRepository.findById(eq(folderId))).thenReturn(Optional.empty());

        // when / then
        assertThrows(NoResultException.class, () ->
                dataSourceService.createDataSource(1, "https://x", folderId)
        );
    }

    @Test
    @DisplayName("폴더 생성 실패 - folderId=null이고 default 폴더를 못 찾으면 예외")
    void createDataSource_defaultFolderNotFound() {
        // given
        int currentMemberId = 10;

        PersonalArchive pa = new PersonalArchive(new Member("u1","p", Provider.KAKAO,null));
        when(personalArchiveRepository.findByMemberId(eq(currentMemberId)))
                .thenReturn(Optional.of(pa));
        when(folderRepository.findByArchiveIdAndIsDefaultTrue(anyInt()))
                .thenReturn(Optional.empty());

        // when / then
        assertThrows(NoResultException.class, () ->
                dataSourceService.createDataSource(currentMemberId, "https://x", null)
        );
    }

}
