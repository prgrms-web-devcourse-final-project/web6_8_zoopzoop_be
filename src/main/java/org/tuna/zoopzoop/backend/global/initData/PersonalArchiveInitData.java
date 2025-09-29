package org.tuna.zoopzoop.backend.global.initData;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;

import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.seed.enabled", havingValue = "true")
public class PersonalArchiveInitData {

    private final MemberRepository memberRepository;
    private final FolderRepository folderRepository;
    private final DataSourceRepository dataSourceRepository;

    @Autowired @Lazy
    private PersonalArchiveInitData self;

    @Bean
    ApplicationRunner archiveInitRunner() {
        return args -> self.initAll(); // 프록시 경유
    }

    @Transactional
    public void initAll() {
        System.out.println(">>> seed initAll start");

        final String providerKeyEmail = "kjjeaus@gmail.com";

        Member member = memberRepository
                .findByProviderAndProviderKey(Provider.KAKAO, providerKeyEmail)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .name("kjjeaus")
                                .providerKey(providerKeyEmail)
                                .provider(Provider.KAKAO)
                                .profileImageUrl("https://img.example.com/profile.png")
                                .build()
                ));

        PersonalArchive pa = member.getPersonalArchive();

        folderRepository.findByArchiveIdAndName(pa.getArchive().getId(), "default")
                .orElseGet(() -> {
                    Folder df = pa.getArchive().getFolders().stream()
                            .filter(Folder::isDefault)
                            .findFirst()
                            .orElse(new Folder("default"));
                    df.setArchive(pa.getArchive());
                    df.setDefault(true);
                    return folderRepository.save(df);
                });

        for (String name : List.of("inbox","research","ai","reading-list")) {
            folderRepository.findByArchiveIdAndName(pa.getArchive().getId(), name)
                    .orElseGet(() -> {
                        Folder f = new Folder(name);
                        f.setDefault(false);
                        f.setArchive(pa.getArchive());
                        return folderRepository.save(f);
                    });
        }

        List<Folder> persistedFolders = folderRepository.findAllByArchiveId(pa.getArchive().getId());

        for (Folder folder : persistedFolders) {
            for (int i = 1; i <= 3; i++) {
                String title = folder.getName() + "-자료" + i;
                if (dataSourceRepository.findByFolderIdAndTitle(folder.getId(), title).isPresent()) continue;

                DataSource ds = new DataSource();
                ds.setFolder(folder);
                ds.setTitle(title);
                ds.setSummary("초기 목데이터");
                ds.setDataCreatedDate(LocalDate.now().minusDays(i));
                ds.setSourceUrl("https://example.com/" + folder.getName() + "/" + i);
                ds.setImageUrl("https://example.com/img/" + i + ".png");
                ds.setSource("Seed");
                ds.setCategory(Category.IT);
                ds.setActive(true);

                dataSourceRepository.save(ds);
            }
        }

        System.out.println(">>> seed initAll end");
    }

}
