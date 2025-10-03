package org.tuna.zoopzoop.backend.domain.space.archive.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.test.util.ReflectionTestUtils;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.SharingArchive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.service.DataSourceService;
import org.tuna.zoopzoop.backend.domain.space.membership.entity.Membership;
import org.tuna.zoopzoop.backend.domain.space.membership.enums.Authority;
import org.tuna.zoopzoop.backend.domain.space.membership.repository.MembershipRepository;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.repository.SpaceRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceDataSourceServiceTest {

    @Mock private DataSourceService domain;
    @Mock private DataSourceRepository dataSourceRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private SpaceRepository spaceRepository;
    @Mock private MembershipRepository membershipRepository;

    @InjectMocks private SpaceDataSourceService app;

    private Space space(int id, int archiveId) {
        Space s = new Space();
        ReflectionTestUtils.setField(s, "id", id);
        SharingArchive sa = new SharingArchive();
        Archive a = new Archive();
        ReflectionTestUtils.setField(a, "id", archiveId);
        sa.archive = a;
        sa.setSpace(s);
        s.setSharingArchive(sa);
        return s;
    }
    private Folder folder(int id, int archiveId, boolean def) {
        Folder f = new Folder();
        ReflectionTestUtils.setField(f, "id", id);
        Archive a = new Archive();
        ReflectionTestUtils.setField(a, "id", archiveId);
        f.setArchive(a);
        f.setDefault(def);
        return f;
    }
    private Membership ms(Space s, Authority auth) {
        Membership m = new Membership();
        m.setSpace(s);
        m.setAuthority(auth);
        return m;
    }

    // ---------------------- Import (개인→공유) ----------------------
    @Test
    @DisplayName("importFromPersonal: 권한 검증 + 복제 생성 호출")
    void importFromPersonal_ok() {
        int requester = 7;
        Space sp = space(11, 100);
        when(spaceRepository.findById(11)).thenReturn(Optional.of(sp));
        when(membershipRepository.findByMemberIdAndSpaceId(requester, 11))
                .thenReturn(Optional.of(ms(sp, Authority.READ_WRITE)));
        when(folderRepository.findByArchiveIdAndIsDefaultTrue(100)).thenReturn(Optional.of(folder(55, 100, true)));

        DataSource personal = new DataSource();
        personal.setTitle("T");
        personal.setSummary("S");
        personal.setSourceUrl("U");
        personal.setImageUrl("I");
        personal.setSource("SRC");
        personal.setCategory(Category.IT);
        personal.setDataCreatedDate(LocalDate.of(2024,1,2));
        when(dataSourceRepository.findByIdAndMemberId(9, requester)).thenReturn(Optional.of(personal));

        when(domain.create(eq(55), any(DataSourceService.CreateCmd.class))).thenReturn(777);

        int created = app.importFromPersonal(requester, "11", 9, 0);

        assertThat(created).isEqualTo(777);
        verify(domain).create(eq(55), argThat(c ->
                "T".equals(c.title()) &&
                        "S".equals(c.summary()) &&
                        "U".equals(c.sourceUrl()) &&
                        "I".equals(c.imageUrl()) &&
                        "SRC".equals(c.source()) &&
                        c.category() == Category.IT &&
                        LocalDate.of(2024,1,2).equals(c.dataCreatedDate())
        ));
    }

    @Test
    @DisplayName("importFromPersonal: READ_ONLY 권한 → SecurityException")
    void importFromPersonal_readOnly() {
        Space sp = space(11, 100);
        when(spaceRepository.findById(11)).thenReturn(Optional.of(sp));
        when(membershipRepository.findByMemberIdAndSpaceId(7, 11))
                .thenReturn(Optional.of(ms(sp, Authority.READ_ONLY)));
        assertThrows(SecurityException.class, () -> app.importFromPersonal(7, "11", 9, 0));
    }

    // ---------------------- Delete / Move / Update ----------------------
    @Test
    @DisplayName("deleteOne: 스코프 검증 후 hardDelete 위임")
    void deleteOne_ok() {
        Space sp = space(11, 100);
        when(spaceRepository.findById(11)).thenReturn(Optional.of(sp));
        when(membershipRepository.findByMemberIdAndSpaceId(7, 11))
                .thenReturn(Optional.of(ms(sp, Authority.READ_WRITE)));
        when(dataSourceRepository.findByIdAndArchiveId(5, 100)).thenReturn(Optional.of(new DataSource()));

        int rs = app.deleteOne(7, "11", 5);

        assertThat(rs).isEqualTo(5);
        verify(domain).hardDeleteOne(5);
    }

    @Test
    @DisplayName("moveOne: target=0 → default 폴더 해석 후 이동")
    void moveOne_default() {
        Space sp = space(11, 100);
        when(spaceRepository.findById(11)).thenReturn(Optional.of(sp));
        when(membershipRepository.findByMemberIdAndSpaceId(7, 11))
                .thenReturn(Optional.of(ms(sp, Authority.READ_WRITE)));
        when(folderRepository.findByArchiveIdAndIsDefaultTrue(100)).thenReturn(Optional.of(folder(55, 100, true)));
        when(dataSourceRepository.findByIdAndArchiveId(9, 100)).thenReturn(Optional.of(new DataSource()));
        when(domain.moveOne(9, 55)).thenReturn(DataSourceService.MoveResult.builder().dataSourceId(9).folderId(55).build());

        var rs = app.moveOne(7, "11", 9, 0);

        assertThat(rs.folderId()).isEqualTo(55);
        verify(domain).moveOne(9, 55);
    }

    @Test
    @DisplayName("update: archive 범위 검증 후 domain.update")
    void update_ok() {
        Space sp = space(11, 100);
        when(spaceRepository.findById(11)).thenReturn(Optional.of(sp));
        when(membershipRepository.findByMemberIdAndSpaceId(7, 11))
                .thenReturn(Optional.of(ms(sp, Authority.READ_WRITE)));
        when(dataSourceRepository.findByIdAndArchiveId(7, 100)).thenReturn(Optional.of(new DataSource()));
        when(domain.update(eq(7), any(DataSourceService.UpdateCmd.class))).thenReturn(7);

        var cmd = DataSourceService.UpdateCmd.builder().title(JsonNullable.of("T")).build();
        int rs = app.update(7, "11", 7, cmd);

        assertThat(rs).isEqualTo(7);
        verify(domain).update(7, cmd);
    }
}
