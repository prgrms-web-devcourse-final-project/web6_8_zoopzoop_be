package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceQRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final FolderRepository folderRepository;
    private final DataSourceQRepository dataSourceQRepository;

    // ===== DTOs =====

    @Builder(toBuilder = true)
    public record CreateCmd(
        String title,
        String summary,
        String source,
        String sourceUrl,
        String imageUrl,
        Category category,
        LocalDate dataCreatedDate,
        List<String> tags
    ){}


    @Builder
    public record UpdateCmd (
        JsonNullable<String> title,
        JsonNullable<String> summary,
        JsonNullable<String> source,
        JsonNullable<String> sourceUrl,
        JsonNullable<String> imageUrl,
        JsonNullable<Category> category,
        JsonNullable<List<String>> tags
    ) {}

    @Builder
    public record MoveResult (
        Integer dataSourceId,
        Integer folderId
    ) {}

    // create
    @Transactional
    public int create(int folderId, CreateCmd cmd) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        DataSource ds = new DataSource();
        ds.setFolder(folder);
        ds.setTitle(cmd.title());
        ds.setSummary(cmd.summary());
        ds.setSource(cmd.source());
        ds.setSourceUrl(cmd.sourceUrl());
        ds.setImageUrl(cmd.imageUrl());
        ds.setCategory(cmd.category());
        ds.setDataCreatedDate(cmd.dataCreatedDate() == null ? LocalDate.now() : cmd.dataCreatedDate());
        ds.setActive(true);

        if (cmd.tags() != null) {
            List<Tag> tags = new ArrayList<>();
            for (String t : cmd.tags()) {
                Tag tag = new Tag(t);
                tag.setDataSource(ds);
                tags.add(tag);
            }
            ds.getTags().clear();
            ds.getTags().addAll(tags);
        }

        return dataSourceRepository.save(ds).getId();
    }

    // update
    @Transactional
    public int update(int dataSourceId, UpdateCmd cmd) {
        DataSource ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        if (cmd.title() != null && cmd.title().isPresent()) ds.setTitle(cmd.title().get());
        if (cmd.summary() != null && cmd.summary().isPresent()) ds.setSummary(cmd.summary().get());
        if (cmd.source() != null && cmd.source().isPresent()) ds.setSource(cmd.source().get());
        if (cmd.sourceUrl() != null && cmd.sourceUrl().isPresent()) ds.setSourceUrl(cmd.sourceUrl().get());
        if (cmd.imageUrl() != null && cmd.imageUrl().isPresent()) ds.setImageUrl(cmd.imageUrl().get());
        if (cmd.category() != null && cmd.category().isPresent()) {
            Category v = cmd.category().get();
            if (v != null) ds.setCategory(v);
            else throw new IllegalArgumentException("유효하지 않은 카테고리입니다.");
        }
        if (cmd.category() != null && cmd.category().isPresent()) {
            Category v = cmd.category().get();
            if (v != null) ds.setCategory(v);
        }

        if (cmd.tags() != null && cmd.tags().isPresent()) {
            List<String> tags = cmd.tags().get();
            ds.getTags().clear();
            if (tags != null) {
                for (String t : tags) {
                    Tag tag = new Tag(t);
                    tag.setDataSource(ds);
                    ds.getTags().add(tag);
                }
            }
        }

        return ds.getId();
    }

    // move
    @Transactional
    public MoveResult moveOne(int dataSourceId, int targetFolderId) {
        DataSource ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));
        Folder target = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        // 동일 폴더 이동은 무시
        if (!Objects.equals(ds.getFolder().getId(), target.getId())) {
            ds.setFolder(target);
        }
        return new MoveResult(ds.getId(), target.getId());
    }

    @Transactional
    public void moveMany(List<Integer> ids, int targetFolderId) {
        if (ids == null || ids.isEmpty()) return;
        Folder target = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        List<DataSource> all = dataSourceRepository.findAllById(ids);
        if (all.size() != ids.size()) throw new NoResultException("존재하지 않는 자료 포함");

        for (DataSource ds : all) {
            if (!Objects.equals(ds.getFolder().getId(), target.getId())) {
                ds.setFolder(target);
            }
        }
    }

    // hard delete
    @Transactional
    public void hardDeleteOne(int dataSourceId) {
        DataSource ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));
        dataSourceRepository.delete(ds);
    }

    @Transactional
    public void hardDeleteMany(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<DataSource> list = dataSourceRepository.findAllById(ids);
        if (list.size() != ids.size()) throw new NoResultException("존재하지 않는 자료 포함");
        dataSourceRepository.deleteAll(list);
    }

    // soft delete
    @Transactional
    public int softDeleteMany(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<DataSource> list = dataSourceRepository.findAllById(ids);
        if (list.size() != ids.size()) throw new NoResultException("존재하지 않는 자료 포함");
        int affected = 0;
        for (DataSource ds : list) {
            if (ds.isActive()) {
                ds.setActive(false);
                ds.setDeletedAt(LocalDate.now());
                affected++;
            }
        }
        return affected;
    }

    // restore
    @Transactional
    public int restoreMany(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<DataSource> list = dataSourceRepository.findAllById(ids);
        if (list.size() != ids.size()) throw new NoResultException("존재하지 않는 자료 포함");
        int affected = 0;
        for (DataSource ds : list) {
            if (!ds.isActive()) {
                ds.setActive(true);
                ds.setDeletedAt(null);
                affected++;
            }
        }
        return affected;
    }

    // 검색
    @Transactional
    public Page<DataSourceSearchItem> searchInArchive(Integer archiveId, DataSourceSearchCondition cond, Pageable pageable) {
        return dataSourceQRepository.searchInArchive(archiveId, cond, pageable);
    }
}
