package org.tuna.zoopzoop.backend.domain.datasource.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.enums.ArchiveType;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceDto;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceQRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DataSourceServiceTest {

    @Mock DataSourceRepository dataSourceRepository;
    @Mock FolderRepository folderRepository;
    @Mock TagRepository tagRepository;
    @Mock DataProcessorService dataProcessorService;
    @Mock DataSourceQRepository dataSourceQRepository;

    @InjectMocks DataSourceService dataSourceService;

    // 테스트용 Archive (공유 아카이브 스코프)
    private Archive archive() {
        Archive a = new Archive(ArchiveType.SHARED);
        ReflectionTestUtils.setField(a, "id", 300);
        return a;
    }

    @Test
    @DisplayName("[Archive] folderId=null → archive default 폴더 생성 경로")
    void create_default_in_archive() throws Exception {
        var a = archive();

        var defaultFolder = new Folder("default");
        ReflectionTestUtils.setField(defaultFolder, "id", 700);

        when(folderRepository.findByArchiveIdAndIsDefaultTrue(300))
                .thenReturn(Optional.of(defaultFolder));
        when(tagRepository.findDistinctTagNamesByFolderId(700))
                .thenReturn(List.of("Team","Research"));
        when(dataProcessorService.process(anyString(), anyList()))
                .thenReturn(new DataSourceDto("t","s", LocalDate.now(), "u", null, "src", Category.IT, List.of("k1")));
        when(dataSourceRepository.save(any()))
                .thenAnswer(inv -> { var ds = (DataSource) inv.getArgument(0); ReflectionTestUtils.setField(ds,"id",123); return ds; });

        int id = dataSourceService.createDataSource(a, "https://x", null);

        org.assertj.core.api.Assertions.assertThat(id).isEqualTo(123);
        verify(folderRepository).findByArchiveIdAndIsDefaultTrue(300);
    }

    @Test
    @DisplayName("[Archive] 삭제: findByIdAndArchiveIdForParticipant 호출 검증")
    void delete_one_in_archive() {
        // ※ 기존 주석/표시 유지. 실제 호출은 findByIdAndArchiveId 로 변경됨.
        var a = archive();
        int id = 5;

        DataSource ds = new DataSource();
        when(dataSourceRepository.findByIdAndArchiveId(id, 300))
                .thenReturn(Optional.of(ds));

        int deleted = dataSourceService.deleteById(a, id);

        org.assertj.core.api.Assertions.assertThat(deleted).isEqualTo(id);
        verify(dataSourceRepository).delete(ds);
    }

    @Test
    @DisplayName("[Archive] 다건 이동: findExistingIdsInArchiveForParticipant → findAllByIdIn")
    void move_many_in_archive() {
        // ※ 기존 주석/표시 유지. 실제 호출은 findExistingIdsInArchive 로 변경됨.
        var a = archive();

        Integer toFolderId = 777;
        List<Integer> ids = List.of(1,2);

        Folder to = new Folder("to");
        ReflectionTestUtils.setField(to,"id",toFolderId);

        DataSource d1 = new DataSource(); ReflectionTestUtils.setField(d1,"id",1);
        DataSource d2 = new DataSource(); ReflectionTestUtils.setField(d2,"id",2);

        // target 폴더는 같은 archive 소속이어야 함
        when(folderRepository.findByIdAndArchiveId(toFolderId, 300)).thenReturn(Optional.of(to));

        // 소속 검증
        when(dataSourceRepository.findExistingIdsInArchive(300, ids)).thenReturn(ids);

        // 실제 엔티티 조회
        when(dataSourceRepository.findAllByIdIn(ids)).thenReturn(List.of(d1, d2));

        dataSourceService.moveDataSources(a, toFolderId, ids);

        org.assertj.core.api.Assertions.assertThat(d1.getFolder()).isEqualTo(to);
        org.assertj.core.api.Assertions.assertThat(d2.getFolder()).isEqualTo(to);
    }
}
