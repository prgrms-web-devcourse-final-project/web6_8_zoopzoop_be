package org.tuna.zoopzoop.backend.domain.space.space.service;

import jakarta.persistence.NoResultException;
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

    // ======================== 스페이스 조회 ======================== //

    /**
     * 스페이스 ID로 스페이스 조회
     * @param spaceId 스페이스 ID
     * @return 조회된 스페이스
     * @throws NoResultException 스페이스가 존재하지 않을 경우
     */
    public Space findById(Integer spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 스페이스입니다."));
    }

    /**
     * 스페이스 이름으로 스페이스 조회
     * @param name 스페이스 이름
     * @return 조회된 스페이스
     * @throws NoResultException 스페이스가 존재하지 않을 경우
     */
    public Space findByName(String name) {
        return spaceRepository.findByName(name)
                .orElseThrow(() -> new NoResultException("존재하지 않는 스페이스입니다."));
    }

    // ======================== 스페이스 생성/수정/삭제 ======================== //

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

    /**
     * 스페이스 삭제 (hard delete)
     * @param spaceId 스페이스 ID
     * @return 삭제된 스페이스 이름
     * @throws IllegalArgumentException 스페이스가 존재하지 않을 경우
     */
    public String deleteSpace(Integer spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 스페이스입니다."));
        String spaceName = space.getName();

        spaceRepository.delete(space);

        return spaceName;
    }

    /**
     * 스페이스 이름 변경
     * @param spaceId 스페이스 ID
     * @param name 새로운 스페이스 이름
     * @return 변경된 스페이스
     * @throws IllegalArgumentException 스페이스가 존재하지 않을 경우
     * @throws DuplicateSpaceNameException 새로운 스페이스 이름이 중복될 경우
     */
    public Space updateSpaceName(Integer spaceId, @NotBlank @Length(max = 50) String name) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 스페이스입니다."));

        space.setName(name);

        try{
            return spaceRepository.saveAndFlush(space);
        }catch (DataIntegrityViolationException e) {
            throw new DuplicateSpaceNameException("이미 존재하는 스페이스 이름입니다.");
        } catch (Exception e) {
            throw e;
        }
    }
}
