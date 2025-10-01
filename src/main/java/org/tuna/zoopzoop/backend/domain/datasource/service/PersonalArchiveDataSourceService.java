package org.tuna.zoopzoop.backend.domain.datasource.service;

import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.Archive;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.PersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.archive.repository.PersonalArchiveRepository;
import org.tuna.zoopzoop.backend.domain.datasource.dto.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalArchiveDataSourceService {

    private final PersonalArchiveRepository personalArchiveRepository;
    private final DataSourceService dataSourceService; // 공통(Archive) 서비스

    private Archive getArchive(Integer memberId) {
        return personalArchiveRepository.findByMemberId(memberId)
                .map(PersonalArchive::getArchive)
                .orElseThrow(() -> new NoResultException("개인 아카이브를 찾을 수 없습니다."));
    }

    @Transactional
    public int create(Integer memberId, String sourceUrl, Integer folderIdOrNull) {
        return dataSourceService.createDataSource(getArchive(memberId), sourceUrl, folderIdOrNull);
    }

    @Transactional
    public int deleteOne(Integer memberId, Integer dataSourceId) {
        return dataSourceService.deleteById(getArchive(memberId), dataSourceId);
    }

    @Transactional
    public void deleteMany(Integer memberId, List<Integer> ids) {
        dataSourceService.deleteMany(getArchive(memberId), ids);
    }

    @Transactional
    public int softDelete(Integer memberId, List<Integer> ids) {
        return dataSourceService.softDelete(getArchive(memberId), ids);
    }

    @Transactional
    public int restore(Integer memberId, List<Integer> ids) {
        return dataSourceService.restore(getArchive(memberId), ids);
    }

    @Transactional
    public DataSourceService.MoveResult moveOne(Integer memberId, Integer dataSourceId, Integer targetFolderIdOrNull) {
        return dataSourceService.moveDataSource(getArchive(memberId), dataSourceId, targetFolderIdOrNull);
    }

    @Transactional
    public void moveMany(Integer memberId, Integer targetFolderIdOrNull, List<Integer> ids) {
        dataSourceService.moveDataSources(getArchive(memberId), targetFolderIdOrNull, ids);
    }

    @Transactional
    public Integer update(Integer memberId, Integer dataSourceId, String title, String summary) {
        return dataSourceService.updateDataSource(getArchive(memberId), dataSourceId, title, summary);
    }

    @Transactional
    public Page<DataSourceSearchItem> search(Integer memberId, DataSourceSearchCondition cond, Pageable pageable) {
        return dataSourceService.search(getArchive(memberId), cond, pageable);
    }
}
