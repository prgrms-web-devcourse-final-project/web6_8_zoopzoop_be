package org.tuna.zoopzoop.backend.domain.news.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tuna.zoopzoop.backend.domain.archive.folder.dto.FolderResponse;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.FolderService;
import org.tuna.zoopzoop.backend.domain.archive.folder.service.PersonalArchiveFolderService;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.service.MemberService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NewsServiceTest {
    @Autowired
    private NewsService newsService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FolderService folderService;

    @Autowired
    private PersonalArchiveFolderService personalArchiveFolderService;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    private Integer newsFolderId;

    private final Map<Integer, List<Tag>> tags = Map.ofEntries(
            Map.entry(1, List.of(new Tag("A"), new Tag("B"), new Tag("E"))),
            Map.entry(2, List.of(new Tag("B"), new Tag("E"), new Tag("F"))),
            Map.entry(3, List.of(new Tag("E"), new Tag("F"))),
            Map.entry(4, List.of(new Tag("A"), new Tag("D"), new Tag("E"))),
            Map.entry(5, List.of(new Tag("B"), new Tag("F"))),
            Map.entry(6, List.of(new Tag("E"), new Tag("F"), new Tag("B"))),
            Map.entry(7, List.of(new Tag("D"), new Tag("E"))),
            Map.entry(8, List.of(new Tag("B"), new Tag("E"))),
            Map.entry(9, List.of(new Tag("F"), new Tag("E"))),
            Map.entry(10, List.of(new Tag("C"), new Tag("E")))
    );
    // A = 2회, B = 5회, C = 1회, D = 2회, E = 9회, F = 5회

    private DataSource buildDataSource(String title, Folder folder, String sourceUrl, List<Tag> tags) {
        DataSource ds = new DataSource();
        ds.setFolder(folder);
        ds.setSourceUrl(sourceUrl);
        ds.setTitle(title);
        ds.setSource("www.examplesource.com");
        ds.setSummary("설명");
        ds.setImageUrl("www.example.com/img");
        ds.setDataCreatedDate(LocalDate.now());
        ds.setTags(tags);
        ds.setCategory(Category.ENVIRONMENT);
        ds.setActive(true);
        return dataSourceRepository.save(ds);
    }

    @AfterEach
    void cleanUp() {
        memberRepository.deleteAll();
    }

    @BeforeEach
    public void setUp() {
        Member member = memberService.createMember(
                "newsServiceTestMember",
                "url",
                "newsServiceTestKey",
                Provider.KAKAO
        );

        FolderResponse folderResponse = personalArchiveFolderService.createFolder(member.getId(), "newServiceTestFolder");
        newsFolderId = folderResponse.folderId();

        Folder folder = folderRepository.findById(folderResponse.folderId()).orElse(null);

        for(int i = 1; i <= 10; i++) {
            buildDataSource(String.valueOf(i), folder, String.valueOf(i), tags.get(i));
        }
    }

    @Test
    @DisplayName("태그 빈도 수 추출 테스트")
    void DataSourceExtractTagsTest(){
        Member member = memberService.findByProviderKey("newsServiceTestKey");
        List<String> frequency = newsService.getTagFrequencyFromFiles(member.getId(), newsFolderId);

        assertEquals("E", frequency.get(0));
        assertEquals("B", frequency.get(1));
        assertEquals("F", frequency.get(2));
    }
}
