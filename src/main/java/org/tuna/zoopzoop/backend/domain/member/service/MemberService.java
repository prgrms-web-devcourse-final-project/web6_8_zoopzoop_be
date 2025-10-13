package org.tuna.zoopzoop.backend.domain.member.service;

import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.tuna.zoopzoop.backend.domain.datasource.repository.DataSourceRepository;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.entity.MemberDocument;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberSearchRepository;
import org.tuna.zoopzoop.backend.global.aws.S3Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberSearchRepository memberSearchRepository;
    private final S3Service s3Service;
    private final TagRepository tagRepository;
    private final DataSourceRepository dataSourceRepository;

    //회원 조회 관련
    public Member findById(Integer id) {
        return memberRepository.findById(id).orElseThrow(() ->
                new NoResultException(id + " id를 가진 사용자를 찾을 수 없습니다.")
        );
    }
    public Member findByName(String name){
        return memberRepository.findByName(name).orElseThrow(() ->
                new NoResultException(name + " 이름을 가진 사용자를 찾을 수 없습니다.")
        );
    }
    public Member findByKakaoKey(String key){
        return memberRepository.findByProviderAndProviderKey(Provider.KAKAO, key).orElseThrow(() ->
                new NoResultException(key + " 카카오 키를 가진 사용자를 찾을 수 없습니다.")
        );
    }

    public Member findByGoogleKey(String key){
        return memberRepository.findByProviderAndProviderKey(Provider.GOOGLE, key).orElseThrow(() ->
                new NoResultException(key + " 구글 키를 가진 사용자를 찾을 수 없습니다.")
        );
    }

    public Member findByProviderKey(String providerKey) {
        return memberRepository.findByProviderKey(providerKey).orElseThrow(() ->
                new NoResultException(providerKey + " 해당 키를 가진 사용자를 찾을 수 없습니다.")
        );
    }

    public Optional<Member> findOptionalByName(String name) {
        return memberRepository.findByName(name);
    }

//    public Member findByEmail(String email){
//        return memberRepository.findByEmail(email).orElseThrow(() ->
//                new NoResultException(email + " 이메일을 가진 사용자를 찾을 수 없습니다.")
//        );
//    }
    public List<Member> findAll(){ return memberRepository.findAll(); }
    public List<Member> findAllActive(){ return memberRepository.findByActiveTrue(); }
    public List<Member> findAllInactive(){ return memberRepository.findByActiveFalse(); }
    //빈 List를 전달하는 경우, 예외 처리를 할 지는 고민해봐야 할 사항.

    //회원 생성/정보 수정 관련
    @Transactional
    public Member createMember(String name, String profileUrl, String key, Provider provider){
        if(memberRepository.findByName(name).isPresent()) {
            throw new DataIntegrityViolationException("이미 사용중인 이름입니다.");
        }

        Member member = Member.builder()
                .name(generateUniqueUserNameTag(name))
                .profileImageUrl(profileUrl)
                .providerKey(key)
                .provider(provider)
                .build();

        Member saved = memberRepository.save(member);

        // ElasticSearch용 document 생성.
        createDocument(member);
        return saved;
    }

    //사용자 이름 수정
    @Transactional
    public void updateMemberName(Member member, String newName){
        if(memberRepository.findByName(newName).isPresent()) {
            throw new DataIntegrityViolationException("이미 사용중인 이름입니다.");
        }
        member.updateName(generateUniqueUserNameTag(newName));
        memberRepository.save(member);

        // ElasticSearch용 document 생성.
        createDocument(member);
    }

    @Transactional
    public void updateMemberProfileUrl(Member member, MultipartFile file){
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = "profile/" + member.getId() + "/profile." + extension;
        try {
            String newUrl = s3Service.upload(file, fileName);
            member.updateProfileUrl(newUrl);
            memberRepository.save(member);

            // ElasticSearch용 document 생성.
            createDocument(member);
        } catch (IOException e) {
            throw new IllegalArgumentException("잘못된 파일 입력입니다.");
        }
    }

    @Transactional
    public void updateMemberProfile(Member member, String newName, MultipartFile file){
        if(memberRepository.findByName(newName).isPresent()) {
            throw new DataIntegrityViolationException("이미 사용중인 이름입니다.");
        }
        member.updateName(generateUniqueUserNameTag(newName));
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = "profile/" + member.getId() + "/profile." + extension;
        try {
            String newUrl = s3Service.upload(file, fileName);
            member.updateProfileUrl(newUrl);
            memberRepository.save(member);

            // ElasticSearch용 document 생성.
            createDocument(member);
        } catch (IOException e) {
            throw new IllegalArgumentException("잘못된 파일 입력입니다.");
        }
    }


    //회원 삭제/복구 관련
    public void softDeleteMember(Member member){ member.deactivate(); }
    @Transactional
    public void hardDeleteMember(Member member){
        Integer memberId = member.getId();

        // 관련 데이터 정리
        tagRepository.bulkDeleteTagsByMemberId(memberId);
        dataSourceRepository.bulkDeleteByMemberId(memberId);

        // 회원 삭제
        memberRepository.delete(member);

        // Elastic Search 인덱스에서 회원 삭제.
        memberSearchRepository.deleteById(memberId);
    }

    //soft-delete한 회원 복구
    public void restoreMember(Member member){ member.activate(); }

    //사용자 이름에 UUID 난수를 맨 앞 5개만 뗴서 붙임.
    private String generateUniqueUserNameTag(String baseName) {
        String candidate;
        do {
            String tag = UUID.randomUUID().toString().substring(0, 5);
            candidate = baseName + "#" + tag;
        } while(memberRepository.existsByName(candidate));
        return candidate;
    }

    // ElasticSearch용 document 생성 메소드.
    private void createDocument(Member member){
        MemberDocument doc = new MemberDocument();
        doc.setId(member.getId());

        String name = member.getName();
        String nameOnly = name.contains("#") ? name.substring(0, name.indexOf("#")) : name;

        doc.setNameOnly(nameOnly);
        doc.setNameWithTag(name);
        doc.setProfileImageUrl(member.getProfileImageUrl());

        memberSearchRepository.save(doc);
    }
}
