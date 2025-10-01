package org.tuna.zoopzoop.backend.domain.datasource.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PersonalArchiveDataSourceServiceTest {

    @Mock PersonalArchiveRepository personalArchiveRepository;
    @Mock DataSourceService archiveScopedService; // 공통(Archive 스코프) 서비스

    @InjectMocks PersonalArchiveDataSourceService personalService;

    @Test
    @DisplayName("[Personal] memberId → personalArchiveId resolve 후 공통 서비스 위임")
    void create_resolve_and_delegate() {
        int memberId = 7;

        var member = new Member("u","p", Provider.KAKAO, null);
        var pa = new PersonalArchive(member);
        // 개인 아카이브 엔티티 id는 테스트 본질과 무관. 퍼사드는 Archive 객체 자체를 공통 서비스에 넘김.
        ReflectionTestUtils.setField(pa,"id",111);

        when(personalArchiveRepository.findByMemberId(memberId)).thenReturn(Optional.of(pa));
        when(archiveScopedService.createDataSource(any(Archive.class), eq("https://x"), isNull()))
                .thenReturn(999);

        int id = personalService.create(memberId, "https://x", null);

        org.assertj.core.api.Assertions.assertThat(id).isEqualTo(999);

        // 넘겨준 Archive 인스턴스를 캡처해 검증(선택)
        ArgumentCaptor<Archive> archiveCaptor = ArgumentCaptor.forClass(Archive.class);
        verify(archiveScopedService).createDataSource(archiveCaptor.capture(), eq("https://x"), isNull());
        org.assertj.core.api.Assertions.assertThat(archiveCaptor.getValue()).isSameAs(pa.getArchive());
    }
}
