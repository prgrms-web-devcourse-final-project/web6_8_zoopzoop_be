package org.tuna.zoopzoop.backend.domain.space.archive.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class SpaceArchiveDataSourceServiceTest {

    @Mock
    SpaceService spaceService;
    @Mock
    MembershipService membershipService;
    @Mock
    DataSourceService archiveScopedService;

    @InjectMocks
    SpaceArchiveDataSourceService spaceFacade;

//    @Test
//    @DisplayName("[Space] 권한 검증 후 공통 서비스 위임")
//    void create_in_space_delegates() {
//        int requesterId = 10, spaceId = 100, archiveId = 300;
//
//        when(spaceService.getArchiveIdBySpaceId(spaceId)).thenReturn(archiveId);
//        when(membershipService.isMemberOf(spaceId, requesterId)).thenReturn(true);
//        when(archiveScopedService.createDataSourceInArchive(archiveId, "https://x", 999)).thenReturn(1234);
//
//        int id = spaceFacade.createDataSource(requesterId, spaceId, "https://x", 999);
//
//        org.assertj.core.api.Assertions.assertThat(id).isEqualTo(1234);
//        verify(archiveScopedService).createDataSourceInArchive(archiveId, "https://x", 999);
//    }
}
