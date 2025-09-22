package org.tuna.zoopzoop.backend.global.initData;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.space.repository.SpaceRepository;

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

    }
}
