package org.tuna.zoopzoop.backend.domain.datasource.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.archive.archive.entity.QPersonalArchive;
import org.tuna.zoopzoop.backend.domain.archive.folder.entity.QFolder;
import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;
import org.tuna.zoopzoop.backend.domain.datasource.entity.QDataSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PersonalDataSourceRepositoryImpl implements PersonalDataSourceRepositoryCustom {

    private final JPAQueryFactory qf;

    @Override
    public Optional<DataSource> findByIdAndMemberId(Integer id, Integer memberId) {
        var d = QDataSource.dataSource;
        var f = QFolder.folder;
        var pa = QPersonalArchive.personalArchive;

        DataSource one = qf.selectFrom(d)
                .join(d.folder, f)
                .join(pa).on(pa.archive.eq(f.archive))
                .where(d.id.eq(id).and(pa.member.id.eq(memberId)))
                .fetchOne();
        return Optional.ofNullable(one);
    }

    @Override
    public List<Integer> findExistingIdsInMember(Integer memberId, Collection<Integer> ids) {
        var d = QDataSource.dataSource;
        var f = QFolder.folder;
        var pa = QPersonalArchive.personalArchive;

        return qf.select(d.id)
                .from(d)
                .join(d.folder, f)
                .join(pa).on(pa.archive.eq(f.archive))
                .where(pa.member.id.eq(memberId).and(d.id.in(ids)))
                .fetch();
    }
}
