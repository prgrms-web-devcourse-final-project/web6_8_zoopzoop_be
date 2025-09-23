package org.tuna.zoopzoop.backend.domain.space.space.service;

import jakarta.persistence.NoResultException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.exception.DuplicateSpaceNameException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class SpaceServiceTest {
    @Autowired
    private SpaceService spaceService;

    @BeforeEach
    void setUp() {
        spaceService.createSpace("기존 스페이스 1_forSpaceServiceTest");
        spaceService.createSpace("기존 스페이스 2_forSpaceServiceTest");
    }

    // ============================= CREATE ============================= //
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

    // ============================= DELETE ============================= //

    @Test
    @DisplayName("스페이스 삭제 - 성공")
    void deleteSpace_Success() {
        // Given
        Space space = spaceService.findByName("기존 스페이스 1_forSpaceServiceTest");
        Integer spaceId = space.getId();
        String spaceName = space.getName();

        // When
        String deletedSpaceName = spaceService.deleteSpace(spaceId);

        // Then
        Assertions.assertThat(deletedSpaceName).isEqualTo(spaceName);
        assertThatThrownBy(() -> spaceService.getSpaceById(spaceId))
                .isInstanceOf(NoResultException.class);
    }

    @Test
    @DisplayName("스페이스 삭제 - 실패 : 존재하지 않는 스페이스")
    void deleteSpace_Fail_NotFound() {
        // Given
        Integer nonExistentSpaceId = 9999;

        // When & Then
        assertThatThrownBy(() -> spaceService.deleteSpace(nonExistentSpaceId))
                .isInstanceOf(NoResultException.class);
    }

    // ============================= Modify ============================= //

    @Test
    @DisplayName("스페이스 이름 변경 - 성공")
    void updateSpaceName_Success() {
        // Given
        Space space = spaceService.findByName("기존 스페이스 1_forSpaceServiceTest");
        Integer spaceId = space.getId();
        String newName = "변경된 스페이스 이름_forSpaceServiceTest";

        // When
        Space updatedSpace = spaceService.updateSpaceName(spaceId, newName);

        // Then
        Assertions.assertThat(updatedSpace).isNotNull();
        Assertions.assertThat(updatedSpace.getId()).isEqualTo(spaceId);
        Assertions.assertThat(updatedSpace.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("스페이스 이름 변경 - 실패 : 존재하지 않는 스페이스")
    void updateSpaceName_Fail_NotFound() {
        // Given
        Integer nonExistentSpaceId = 9999;
        String newName = "변경된 스페이스 이름";

        // When & Then
        assertThatThrownBy(() -> spaceService.updateSpaceName(nonExistentSpaceId, newName))
                .isInstanceOf(NoResultException.class);
    }

    @Test
    @DisplayName("스페이스 이름 변경 - 실패 : 중복된 스페이스 이름")
    void updateSpaceName_Fail_DuplicateName() {
        // Given
        Space space = spaceService.findByName("기존 스페이스 1_forSpaceServiceTest");
        Integer spaceId = space.getId();
        String duplicateName = "기존 스페이스 2_forSpaceServiceTest";

        // When & Then
        assertThatThrownBy(() -> spaceService.updateSpaceName(spaceId, duplicateName))
                .isInstanceOf(DuplicateSpaceNameException.class);
    }

}