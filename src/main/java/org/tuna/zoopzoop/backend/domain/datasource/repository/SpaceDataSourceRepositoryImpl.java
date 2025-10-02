package org.tuna.zoopzoop.backend.domain.datasource.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.QFolder;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.QDataSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpaceDataSourceRepositoryImpl implements SpaceDataSourceRepositoryCustom {

    private final JPAQueryFactory qf;

    @Override
    public Optional<DataSource> findByIdAndArchiveId(Integer id, Integer archiveId) {
        var d = QDataSource.dataSource;
        var f = QFolder.folder;
        DataSource one = qf.selectFrom(d)
                .join(d.folder, f)
                .where(d.id.eq(id).and(f.archive.id.eq(archiveId)))
                .fetchOne();
        return Optional.ofNullable(one);
    }

    @Override
    public List<Integer> findExistingIdsInArchive(Integer archiveId, Collection<Integer> ids) {
        var d = QDataSource.dataSource;
        var f = QFolder.folder;
        return qf.select(d.id)
                .from(d)
                .join(d.folder, f)
                .where(f.archive.id.eq(archiveId).and(d.id.in(ids)))
                .fetch();
    }
}
