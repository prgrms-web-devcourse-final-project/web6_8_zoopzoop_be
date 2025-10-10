package org.tuna.zoopzoop.backend.domain.member.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.MemberDocument;

import java.util.List;

public interface MemberSearchRepository extends ElasticsearchRepository<MemberDocument, Integer> {
    List<MemberDocument> findByNameContaining(String name);
}
