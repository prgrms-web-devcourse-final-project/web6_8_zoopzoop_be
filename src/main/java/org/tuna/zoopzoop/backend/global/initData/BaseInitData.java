package org.tuna.zoopzoop.backend.global.initData;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.space.repository.SpaceRepository;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    private final MemberRepository memberRepository;
    private final SpaceRepository spaceRepository;

    @Autowired
    @Lazy
    private BaseInitData self;

    @Bean
    ApplicationRunner initData(){
        return args -> {
            self.initalizeData();
        };
    }

    @Transactional
    @Profile("!test")
    public void initalizeData() {
        try {
            Member member1 = Member.builder()
                    .name("Alice")
                    .email("alice@example.com")
                    .profileImageUrl("https://example.com/alice.png")
                    .build();

            Member member2 = Member.builder()
                    .name("Bob")
                    .email("bob@example.com")
                    .profileImageUrl("https://example.com/bob.png")
                    .build();

            Space space1 = Space.builder()
                    .name("Space1")
                    .active(true)
                    .build();

            Space space2 = Space.builder()
                    .name("Space2")
                    .active(true)
                    .build();

            memberRepository.save(member1);
            memberRepository.save(member2);

            spaceRepository.save(space1);
            spaceRepository.save(space2);
        } catch (Exception e) {
            System.err.println("초기 데이터 생성 중 오류 발생: " + e.getMessage());
        }
    }
}
