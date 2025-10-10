package org.tuna.zoopzoop.backend.domain.space.space.service;

import jakarta.persistence.NoResultException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.space.membership.service.MembershipService;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.exception.DuplicateSpaceNameException;
import org.tuna.zoopzoop.backend.domain.space.space.repository.SpaceRepository;
import org.tuna.zoopzoop.backend.global.aws.S3Service;
import org.tuna.zoopzoop.backend.global.clients.liveblocks.LiveblocksClient;

@Service
@RequiredArgsConstructor
public class SpaceService {
    private final SpaceRepository spaceRepository;
    private final S3Service s3Service;
    private final MembershipService membershipService;
    private final LiveblocksClient liveblocksClient;
    private final TagRepository tagRepository;
    private final DataSourceRepository dataSourceRepository;

    // ======================== 스페이스 조회 ======================== //

    /**
     * 스페이스 ID로 스페이스 조회
     * @param spaceId 스페이스 ID
     * @return 조회된 스페이스
     * @throws NoResultException 스페이스가 존재하지 않을 경우
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional
    public Space createSpace(@NotBlank @Length(max = 50) String name) {
        return createSpace(name, null);
    }

    /**
     * 스페이스 생성
     * @param name 스페이스 이름
     * @param thumbnailUrl 스페이스 썸네일 이미지 URL
     * @return 생성된 스페이스
     */
    @Transactional
    public Space createSpace(@NotBlank @Length(max = 50) String name, String thumbnailUrl) {
        Space newSpace = Space.builder()
                .name(name)
                .thumbnailUrl(thumbnailUrl)
                .build();

        Space savedSpace;
        try{
            savedSpace = spaceRepository.save(newSpace);
        }catch (DataIntegrityViolationException e) {
            throw new DuplicateSpaceNameException("이미 존재하는 스페이스 이름입니다.");
        } catch (Exception e) {
            throw e;
        }

        // Liveblocks에 방 생성 요청
        liveblocksClient.createRoom("space_" + savedSpace.getId());

        return savedSpace;
    }

    /**
     * 스페이스 삭제 (hard delete)
     * @param spaceId 스페이스 ID
     * @return 삭제된 스페이스 이름
     * @throws IllegalArgumentException 스페이스가 존재하지 않을 경우
     */

    @Transactional
    public String deleteSpace(Integer spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 스페이스입니다."));

        String spaceName = space.getName();
        String roomId = "space_" + space.getId();

        // Liveblocks에 방 삭제 요청
        liveblocksClient.deleteRoom(roomId);

        tagRepository.bulkDeleteTagsBySpaceId(spaceId);
        dataSourceRepository.bulkDeleteBySpaceId(spaceId);
        // folder, dashboard membership 등 cascade 설정으로 인해 자동 삭제
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

    @Transactional
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

    /**
     * 스페이스 썸네일 이미지 변경
     * @param spaceId 스페이스 ID
     * @param image 새로운 썸네일 이미지
     * @throws IllegalArgumentException 스페이스가 존재하지 않을 경우
     */
    @Transactional
    public void updateSpaceThumbnail(Integer spaceId, Member requester, MultipartFile image) {
        // 이미지가 null이거나 비어있는 경우 예외 처리
        if(image == null || image.isEmpty()) {
            return;
        }

        // 파일 크기 제한 (5MB)
        if (image.getSize() > (5 * 1024 * 1024))  // 5MB
            throw new IllegalArgumentException("이미지 파일 크기는 5MB를 초과할 수 없습니다.");

        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NoResultException("존재하지 않는 스페이스입니다."));

        if (requester == null) {
            throw new IllegalArgumentException("사용자 정보가 없습니다.");
        }

        if (!membershipService.isMemberJoinedSpace(requester, space)) {
            throw new IllegalArgumentException("스페이스의 구성원이 아닙니다.");
        }

        try {
            //String fileName = "space/" + spaceId + "/thumbnail/" + System.currentTimeMillis() + "_" +
            // S3 저장 시 파일 이름 고정 (덮어쓰기)
            String fileName = "space-thumbnail/space_" + spaceId ;
            String baseImageUrl = s3Service.upload(image, fileName);

            // DB 용으로 현재 시간을 쿼리 파라미터에 추가 (캐시 무효화)
            String finalImageUrl = baseImageUrl + "?v=" + System.currentTimeMillis();

            // DB 갱신
            space.setThumbnailUrl(finalImageUrl);
            spaceRepository.save(space);
        } catch (Exception e) {
            throw new RuntimeException("스페이스 썸네일 이미지 업로드에 실패했습니다.");
        }
    }
}
