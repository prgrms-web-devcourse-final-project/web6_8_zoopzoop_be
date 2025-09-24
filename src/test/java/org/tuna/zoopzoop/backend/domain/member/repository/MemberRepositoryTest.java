package org.tuna.zoopzoop.backend.domain.member.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class MemberRepositoryTest {
    @Autowired
    MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        memberRepository.deleteAll(); // Graph만 삭제
        // 필요하면 다른 Repository도 순서대로 삭제
    }

    @Test
    @DisplayName("Member 저장 시 PersonalArchive + Archive + default 폴더가 자동 생성된다")
    void memberPersistsWithDefaultFolder() {
        // given: Personal Archive 생성 + Personal Archive 생성자가 Archive와 Default 폴더 생성
        Member m = Member.builder()
                .name("alice")
                .providerKey("kakao-123")
                .provider(Provider.KAKAO)
                .profileImageUrl(null)
                .build();

        // when
        Member saved = memberRepository.save(m);

        // then
        var pa = saved.getPersonalArchive();
        assertThat(pa).isNotNull();

        Archive archive = pa.getArchive();
        assertThat(archive).isNotNull();

        List<Folder> folders = archive.getFolders();
        assertThat(folders).isNotEmpty();
        Folder defaultFolder = folders.stream().filter(Folder::isDefault).findFirst().orElse(null);

        assertThat(defaultFolder).isNotNull();
        assertThat(defaultFolder.getName()).isEqualTo("default");
        assertThat(defaultFolder.getArchive()).isSameAs(archive); // 양방향 일관성
    }
}
