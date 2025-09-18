package org.tuna.zoopzoop.backend.domain.datasource.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class DataSource extends BaseEntity {
    //연결된 폴더 id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    //제목
    @Column(nullable = false)
    private String title;

    //요약
    @Column(nullable = false)
    private String summary;

    //소스 데이터의 작성일자
    //DB 저장용 createdDate와 다름.
    @Column(nullable = false)
    private LocalDateTime dataCreatedDate;

    //소스 데이터 URL
    @Column(nullable = false)
    private String sourceUrl;

    //썸네일 이미지 URL
    @Column
    private String thumbnailUrl;

    // 태그 목록
    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();

    // 활성화 여부
    @Column(nullable = false)
    private boolean isActive = true;
}
