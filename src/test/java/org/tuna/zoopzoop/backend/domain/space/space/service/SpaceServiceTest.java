package org.tuna.zoopzoop.backend.domain.space.space.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.space.space.exception.DuplicateSpaceNameException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class SpaceServiceTest {
    @Autowired
    private SpaceService spaceService;

    @Test
    @DisplayName("스페이스 생성 - 성공")
    void createSpace_Success() {
        // Given
        String spaceName = "테스트 스페이스";

        // When
        var createdSpace = spaceService.createSpace(spaceName);

        // Then
        // 생성된 스페이스 검증
        Assertions.assertThat(createdSpace).isNotNull();
        Assertions.assertThat(createdSpace.getId()).isNotNull();
        Assertions.assertThat(createdSpace.getName()).isEqualTo(spaceName);
        Assertions.assertThat(createdSpace.isActive()).isTrue();

        // 연관된 SharingArchive 검증
        Assertions.assertThat(createdSpace.getSharingArchive()).isNotNull();
        Assertions.assertThat(createdSpace.getSharingArchive().getSpace()).isEqualTo(createdSpace);
        Assertions.assertThat(createdSpace.getSharingArchive().getArchive()).isNotNull();
        Assertions.assertThat(createdSpace.getSharingArchive().getArchive().getArchiveType())
                .isEqualTo(org.tuna.zoopzoop.backend.domain.archive.archive.enums.ArchiveType.SHARED);
    }

    @Test
    @DisplayName("스페이스 생성 - 실패 : 중복된 스페이스 이름")
    void createSpace_Fail_DuplicateName() {
        // Given
        String spaceName = "중복 스페이스";
        spaceService.createSpace(spaceName);

        // When & Then
        assertThatThrownBy(() -> spaceService.createSpace(spaceName))
                .isInstanceOf(DuplicateSpaceNameException.class);
    }



}