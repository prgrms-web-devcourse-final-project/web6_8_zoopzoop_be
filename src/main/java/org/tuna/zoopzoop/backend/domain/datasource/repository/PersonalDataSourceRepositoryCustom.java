package org.tuna.zoopzoop.backend.domain.datasource.repository;

import org.tuna.zoopzoop.backend.domain.datasource.entity.DataSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PersonalDataSourceRepositoryCustom {
    Optional<DataSource> findByIdAndMemberId(Integer id, Integer memberId);
    List<Integer> findExistingIdsInMember(Integer memberId, Collection<Integer> ids);
}
