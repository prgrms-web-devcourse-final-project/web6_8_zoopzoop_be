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
    SpaceDataSourceService spaceFacade;

}
