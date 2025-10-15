package org.tuna.zoopzoop.backend.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.member.entity.MemberDocument;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberSearchRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberSearchService {
    private final MemberSearchRepository memberSearchRepository;
    public List<MemberDocument> searchByName(String name) {
        return memberSearchRepository.findByNameOnly(name);
    }
}
