package org.tuna.zoopzoop.backend.domain.space.space.service;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.exception.DuplicateSpaceNameException;
import org.tuna.zoopzoop.backend.domain.space.space.repository.SpaceRepository;

@Service
@RequiredArgsConstructor
public class SpaceService {
    private final SpaceRepository spaceRepository;

    /**
     * 스페이스 생성
     * @param name 스페이스 이름
     * @return 생성된 스페이스
     */
    public Space createSpace(@NotBlank @Length(max = 50) String name) {
        Space newSpace = Space.builder()
                .name(name)
                .build();
        try{
            return spaceRepository.save(newSpace);
        }catch (DataIntegrityViolationException e) {
            throw new DuplicateSpaceNameException("이미 존재하는 스페이스 이름입니다.");
        } catch (Exception e) {
            throw e;
        }
    }
}
