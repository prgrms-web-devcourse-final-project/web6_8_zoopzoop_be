package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.Folder;
import org.tuna.zoopzoop.backend.domain.archive.folder.repository.FolderRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchCondition;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceSearchItem;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Category;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceQRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.global.aws.S3Service;

import java.net.URI;
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
    private final S3Service s3Service;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

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
    ) {
        public static UpdateCmd.UpdateCmdBuilder builderFrom(UpdateCmd base) {
            return UpdateCmd.builder()
                    .title(base.title())
                    .summary(base.summary())
                    .source(base.source())
                    .sourceUrl(base.sourceUrl())
                    .imageUrl(base.imageUrl())
                    .category(base.category())
                    .tags(base.tags());
        }
    }

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
            if (v == null) throw new IllegalArgumentException("유효하지 않은 카테고리입니다.");
            ds.setCategory(v);
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
        deleteOwnedImageIfAny(ds);
        dataSourceRepository.delete(ds);
    }

    @Transactional
    public void hardDeleteMany(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<DataSource> list = dataSourceRepository.findAllById(ids);
        if (list.size() != ids.size()) throw new NoResultException("존재하지 않는 자료 포함");
        for (DataSource ds : list) deleteOwnedImageIfAny(ds);
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

    // search
    @Transactional
    public Page<DataSourceSearchItem> searchInArchive(Integer archiveId, DataSourceSearchCondition cond, Pageable pageable) {
        return dataSourceQRepository.searchInArchive(archiveId, cond, pageable);
    }

    // ===== update: 공통 유틸 =====
    // 이미지 유효성 검사
    public void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
        }
        if (image.getSize() > (5 * 1024 * 1024)) {
            throw new IllegalArgumentException("이미지 파일 크기는 5MB를 초과할 수 없습니다.");
        }
        String ct = image.getContentType();
        if (ct == null || !(ct.equals("image/png") || ct.equals("image/jpeg") || ct.equals("image/webp"))) {
            throw new IllegalArgumentException("이미지 형식은 PNG/JPEG/WEBP만 허용합니다.");
        }
    }

    // 썸네일 S3 키 생성
    public String thumbnailKeyForPersonal(int memberId, int dataSourceId) {
        return "datasource-thumbnail/personal_" + memberId + "/ds_" + dataSourceId;
    }
    public String thumbnailKeyForSpace(int spaceId, int dataSourceId) {
        return "datasource-thumbnail/space_" + spaceId + "/ds_" + dataSourceId;
    }

    // 썸네일 업로드 + URL 반환
    public String uploadThumbnailAndReturnFinalUrl(MultipartFile image, String key) {
        validateImage(image);
        try {
            String baseUrl = s3Service.upload(image, key); // S3 putObject
            return baseUrl + "?v=" + System.currentTimeMillis();
        } catch (Exception e) {
            throw new RuntimeException("썸네일 이미지 업로드에 실패했습니다.");
        }
    }

    // ===== S3 삭제 관련 유틸 =====
    // 소유한 이미지가 있으면 S3에서 삭제
    private void deleteOwnedImageIfAny(DataSource ds) {
        String url = ds.getImageUrl();
        if (url == null || url.isBlank()) return;
        if (!isOurS3Url(url)) return;

        String key = extractKeyFromUrl(url);
        if (key == null || key.isBlank()) return;

        try {
            s3Service.delete(key);
        } catch (Exception ignore) {
            // 파일 삭제 실패로 전체 삭제를 롤백하지 않음
            // 필요하면 warn 로그 추가
        }
    }

    // URL이 우리 S3 버킷의 객체를 가리키는지 검사
    private boolean isOurS3Url(String url) {
        try {
            String noQuery = url.split("\\?")[0];
            URI uri = URI.create(noQuery);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || bucket == null || bucket.isBlank()) return false;

            if (host.startsWith(bucket + ".s3")) return true;

            return host.startsWith("s3.") && path != null && path.startsWith("/" + bucket + "/");
        } catch (Exception e) {
            return false;
        }
    }

    // S3 URL에서 key 추출
    private String extractKeyFromUrl(String url) {
        try {
            String noQuery = url.split("\\?")[0];
            URI uri = URI.create(noQuery);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || path == null) return null;

            // virtual-hosted-style: /<key>
            if (host.startsWith(bucket + ".s3")) return trimLeadingSlash(path);

            // path-style: /{bucket}/{key}
            if (host.startsWith("s3.") && path.startsWith("/" + bucket + "/")) {
                return path.substring(("/" + bucket + "/").length());
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // 문자열 앞의 '/' 제거
    private String trimLeadingSlash(String s) {
        return (s != null && s.startsWith("/")) ? s.substring(1) : s;
    }
}
