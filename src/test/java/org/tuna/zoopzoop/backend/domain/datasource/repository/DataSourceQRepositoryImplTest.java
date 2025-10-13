package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.global.config.QuerydslConfig;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, DataSourceQRepositoryImpl.class})
class DataSourceQRepositoryImplTest {

    @Autowired
    DataSourceQRepositoryImpl dataSourceQRepository;

    @Autowired MemberRepository memberRepository;
    @Autowired PersonalArchiveRepository personalArchiveRepository;
    @Autowired FolderRepository folderRepository;
    @Autowired DataSourceRepository dataSourceRepository;

    Integer memberId;
    Integer defaultFolderId;
    Folder defaultFolder;

    @BeforeEach
    void setUp() {
        // 1) 멤버 저장 (Member 생성자에서 PersonalArchive + Archive + default Folder 객체는 만들어짐)
        Member member = memberRepository.saveAndFlush(
                Member.builder()
                        .name("tester")
                        .providerKey("KAKAO:" + System.nanoTime())
                        .provider(Provider.KAKAO)
                        .profileImageUrl("http://img")
                        .build()
        );
        this.memberId = member.getId();

        // 2) 기본 폴더 가져오기 (아직 비영속일 수 있음)
        Folder defaultFolder = member.getPersonalArchive().getArchive()
                .getFolders().stream()
                .filter(Folder::isDefault)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기본 폴더가 생성되지 않았습니다."));


        defaultFolder = folderRepository.save(defaultFolder);

        this.defaultFolder = defaultFolder;
        this.defaultFolderId = defaultFolder.getId();

        // 4) 시드 데이터 3건 삽입
        DataSource d1 = ds(defaultFolder, "a-note",  "s1",       LocalDate.now().minusDays(2), Category.IT,      List.of("X"));
        DataSource d2 = ds(defaultFolder, "b-spec",  "s2 Hello", LocalDate.now().minusDays(1), Category.SCIENCE, List.of("Y"));
        DataSource d3 = ds(defaultFolder, "c-hello", "s3",       LocalDate.now(),              Category.IT,      List.of("Z"));

        dataSourceRepository.saveAll(List.of(d1, d2, d3));
        dataSourceRepository.flush();
        folderRepository.flush();
    }



    private DataSource ds(Folder f, String title, String sum, LocalDate date, Category cat, List<String> tags) {
        DataSource d = new DataSource();
        d.setFolder(f);
        d.setTitle(title);
        d.setSummary(sum);
        d.setSourceUrl("http://src/" + title);
        d.setImageUrl("http://img/" + title);
        d.setDataCreatedDate(date);
        d.setActive(true);
        d.setCategory(cat);

        if (tags != null) {
            List<Tag> list = tags.stream().map(Tag::new)
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            list.forEach(t -> t.setDataSource(d));
            d.setTags(list);
        }
        return d;
    }

    @Test
    @DisplayName("검색 성공: 정렬 createdAt,desc")
    void sort_createdAt_desc_maps_to_dataCreatedDate_desc() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")));
        DataSourceSearchCondition cond = DataSourceSearchCondition.builder().build();

        Page<DataSourceSearchItem> page = dataSourceQRepository.search(memberId, cond, pageable);

        assertThat(page.getContent()).hasSize(3);
        // 최신(LocalDate.now())가 첫 번째
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("c-hello");
    }

    @Test
    @DisplayName("검색 성공: 정렬 title asc")
    void sort_title_asc() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("title")));
        DataSourceSearchCondition cond = DataSourceSearchCondition.builder().build();

        Page<DataSourceSearchItem> page = dataSourceQRepository.search(memberId, cond, pageable);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("a-note");
        assertThat(page.getContent().get(1).getTitle()).isEqualTo("b-spec");
        assertThat(page.getContent().get(2).getTitle()).isEqualTo("c-hello");
    }

    @Test
    @DisplayName("부분 검색 성공: title contains 'hello'")
    void filter_title_contains() {
        Pageable pageable = PageRequest.of(0, 10);
        DataSourceSearchCondition cond = DataSourceSearchCondition.builder()
                .title("hello")
                .build();

        Page<DataSourceSearchItem> page = dataSourceQRepository.search(memberId, cond, pageable);
        assertThat(page.getContent()).extracting(DataSourceSearchItem::getTitle)
                .containsExactly("c-hello");
    }

    @Test
    @DisplayName("부분 검색 성공: 필터 summary contains 'Hello'")
    void filter_summary_contains() {
        Pageable pageable = PageRequest.of(0, 10);
        DataSourceSearchCondition cond = DataSourceSearchCondition.builder()
                .summary("hello")
                .build();

        Page<DataSourceSearchItem> page = dataSourceQRepository.search(memberId, cond, pageable);
        assertThat(page.getContent()).extracting(DataSourceSearchItem::getSummary)
                .containsExactly("s2 Hello");
    }

    @Test
    @DisplayName("부분 검색 성공: 필터 category contains 'it'")
    void filter_category_contains() {
        Pageable pageable = PageRequest.of(0, 10);
        DataSourceSearchCondition cond = DataSourceSearchCondition.builder()
                .category(Category.IT)
                .build();

        Page<DataSourceSearchItem> page = dataSourceQRepository.search(memberId, cond, pageable);
        assertThat(page.getContent()).allMatch(i -> i.getCategory().equals("IT"));
    }

    @Test
    @DisplayName("검색: default 폴더 검색")
    void filter_folderName_eq() {
        Pageable pageable = PageRequest.of(0, 10);
        DataSourceSearchCondition cond = DataSourceSearchCondition.builder()
                .folderName("default")
                .build();

        Page<DataSourceSearchItem> page = dataSourceQRepository.search(memberId, cond, pageable);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("검색 페이징: page=1, size=2")
    void paging_page1_size2() {
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Order.desc("createdAt")));
        DataSourceSearchCondition cond = DataSourceSearchCondition.builder().build();

        Page<DataSourceSearchItem> page = dataSourceQRepository.search(memberId, cond, pageable);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("검색 페이징: 휴지통 - isActive=true → soft-deleted 제외")
    void qdsl_filter_isActive_true_excludes_trash() {
        DataSource victim = dataSourceRepository.findAll()
                .stream().filter(d -> d.getTitle().equals("c-hello")).findFirst().orElseThrow();
        victim.setActive(false);
        victim.setDeletedAt(LocalDate.now());
        dataSourceRepository.saveAndFlush(victim);

        Pageable pageable = PageRequest.of(0, 10);
        DataSourceSearchCondition cond = DataSourceSearchCondition.builder()
                .isActive(true)
                .build();

        // when
        Page<DataSourceSearchItem> page = dataSourceQRepository.search(memberId, cond, pageable);

        // then: 기존 3건 중 1건이 휴지통 → 2건만 조회
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(DataSourceSearchItem::getTitle)
                .doesNotContain("c-hello");
    }

    @Test
    @DisplayName("검색 페이징: 휴지통 - isActive=false → 휴지통만 노출")
    void qdsl_filter_isActive_false_only_trash() {
        // given: b-spec만 휴지통 처리
        DataSource victim = dataSourceRepository.findAll()
                .stream().filter(d -> d.getTitle().equals("b-spec")).findFirst().orElseThrow();
        victim.setActive(false);
        victim.setDeletedAt(LocalDate.now());
        dataSourceRepository.saveAndFlush(victim);

        Pageable pageable = PageRequest.of(0, 10);
        DataSourceSearchCondition cond = DataSourceSearchCondition.builder()
                .isActive(false)
                .build();

        // when
        Page<DataSourceSearchItem> page = dataSourceQRepository.search(memberId, cond, pageable);

        // then: 오직 b-spec 한 건만
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(DataSourceSearchItem::getTitle)
                .containsExactly("b-spec");
    }
}
