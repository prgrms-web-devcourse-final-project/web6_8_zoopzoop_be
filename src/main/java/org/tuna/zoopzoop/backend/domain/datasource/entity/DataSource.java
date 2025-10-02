package org.tuna.zoopzoop.backend.domain.datasource.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class DataSource extends BaseEntity {
    //연결된 폴더 id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    //제목
    @Column
    private String title;

    //요약
    @Column
    private String summary;

    //소스 데이터의 작성일자
    //DB 저장용 createdDate와 다름.
    @Column
    private LocalDate dataCreatedDate;

    //소스 데이터 URL
    @Column
    private String sourceUrl;

    //썸네일 이미지 URL
    @Column
    private String imageUrl;

    // 자료 출처 (동아일보, Tstory 등등)
    @Column
    private String source;

    // 태그 목록
    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();

    // 카테고리 목록
    @Enumerated(EnumType.STRING) // IT, SCIENCE 등 ENUM 이름으로 저장
    @Column
    private Category category;

    // 활성화 여부
    @Column
    private boolean isActive = true;

    // 삭제 일자
    @Column
    private LocalDate deletedAt;
}
