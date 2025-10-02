package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
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

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final FolderRepository folderRepository;
    private final DataSourceQRepository dataSourceQRepository;

    @Override
    @Transactional
    public int create(int folderId, CreateCmd cmd) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        // (folder, title) 유니크 정책이 있다면 사전 검증(Optional)
        if (cmd.title() != null) {
            dataSourceRepository.findByFolderIdAndTitle(folderId, cmd.title())
                    .ifPresent(d -> { throw new IllegalArgumentException("같은 폴더에 중복 제목이 존재합니다."); });
        }

        DataSource ds = new DataSource();
        ds.setFolder(folder);
        ds.setTitle(cmd.title());
        ds.setSummary(cmd.summary());
        ds.setSourceUrl(cmd.sourceUrl());
        ds.setImageUrl(cmd.imageUrl());
        ds.setSource(cmd.source());
        ds.setCategory(cmd.category());
        ds.setDataCreatedDate(cmd.dataCreatedDate());
        ds.setActive(true);
        ds.setDeletedAt(null);

        if (cmd.tags() != null) {
            for (String name : cmd.tags()) {
                if (name == null) continue;
                Tag tag = Tag.builder().tagName(name).dataSource(ds).build();
                ds.getTags().add(tag);
            }
        }

        return dataSourceRepository.save(ds).getId();
    }

    @Override
    @Transactional
    public MoveResult moveOne(int dataSourceId, int targetFolderId) {
        DataSource ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));
        Folder target = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        // Cross-Archive 금지
        if (!Objects.equals(ds.getFolder().getArchive().getId(), target.getArchive().getId())) {
            throw new IllegalArgumentException("같은 아카이브 내 폴더로만 이동할 수 있습니다.");
        }
        if (Objects.equals(ds.getFolder().getId(), target.getId())) {
            return new MoveResult(ds.getId(), target.getId()); // 멱등
        }
        ds.setFolder(target);
        return new MoveResult(ds.getId(), target.getId());
    }

    @Override
    @Transactional
    public void moveMany(List<Integer> dataSourceIds, int targetFolderId) {
        if (dataSourceIds == null || dataSourceIds.isEmpty())
            throw new IllegalArgumentException("이동할 자료 id가 비어있습니다.");

        Folder target = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 폴더입니다."));

        List<DataSource> list = dataSourceRepository.findAllById(dataSourceIds);
        if (list.size() != dataSourceIds.size())
            throw new NoResultException("요청한 자료 중 존재하지 않는 항목이 있습니다.");

        // Cross-Archive 금지
        for (DataSource ds : list) {
            if (!Objects.equals(ds.getFolder().getArchive().getId(), target.getArchive().getId())) {
                throw new IllegalArgumentException("같은 아카이브 내 폴더로만 이동할 수 있습니다.");
            }
        }
        list.stream()
                .filter(d -> !Objects.equals(d.getFolder().getId(), target.getId()))
                .forEach(d -> d.setFolder(target));
    }

    @Override
    @Transactional
    public int update(int dataSourceId, UpdateCmd cmd) {
        DataSource ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 자료입니다."));

        if (isPresent(cmd.title()))     ds.setTitle(cmd.title().orElse(null));
        if (isPresent(cmd.summary()))   ds.setSummary(cmd.summary().orElse(null));
        if (isPresent(cmd.sourceUrl())) ds.setSourceUrl(cmd.sourceUrl().orElse(null));
        if (isPresent(cmd.imageUrl()))  ds.setImageUrl(cmd.imageUrl().orElse(null));
        if (isPresent(cmd.source()))    ds.setSource(cmd.source().orElse(null));
        if (isPresent(cmd.category()))  ds.setCategory(parseCategoryNullable(cmd.category().orElse(null)));

        if (isPresent(cmd.tags())) {
            List<String> names = cmd.tags().orElse(null);
            ds.getTags().clear();
            if (names != null) {
                for (String n : names) {
                    if (n == null) continue;
                    ds.getTags().add(Tag.builder().tagName(n).dataSource(ds).build());
                }
            }
        }
        return ds.getId();
    }

    @Override
    @Transactional
    public int softDeleteMany(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("삭제할 자료 id 배열이 비어있습니다.");
        return dataSourceRepository.softDeleteAllByIds(ids, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int restoreMany(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("복원할 자료 id 배열이 비어있습니다.");
        return dataSourceRepository.restoreAllByIds(ids);
    }

    @Override
    @Transactional
    public void hardDeleteOne(int id) {
        dataSourceRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void hardDeleteMany(List<Integer> ids) {
        dataSourceRepository.deleteAllByIdInBatch(ids);
    }

    @Override
    public Page<DataSourceSearchItem> searchInArchive(int archiveId, DataSourceSearchCondition cond, Pageable pageable) {
        return dataSourceQRepository.searchInArchive(archiveId, cond, pageable);
    }

    private boolean isPresent(JsonNullable<?> v) {
        return v != null && v.isPresent();
    }

    private Category parseCategoryNullable(String raw) {
        if (raw == null) return null;
        String k = raw.trim();
        if (k.isEmpty()) return null;
        return Category.valueOf(k.toUpperCase(Locale.ROOT));
    }
}

