package org.tuna.zoopzoop.backend.global.initData;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;
import org.tuna.zoopzoop.backend.domain.datasource.ai.service.AiService;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.domain.space.space.repository.SpaceRepository;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    private final MemberRepository memberRepository;
    private final SpaceRepository spaceRepository;
    private final TagRepository tagRepository;

    @Autowired
    @Lazy
    private BaseInitData self;

    @Bean
    ApplicationRunner initData(){
        return args -> {
            self.initalizeData();
            self.initTagData();
        };
    }

    @Transactional
    @Profile("!test")
    public void initalizeData() {

    }

    private final AiService aiService;

    @Transactional
    public void initTagData() {
        if (tagRepository.count() > 0) {
            return;
        }

        Tag tag1 = new Tag(null,"IT");
        Tag tag2 = new Tag(null, "자기소개");
        Tag tag3 = new Tag(null, "이름");

        tagRepository.save(tag1);
        tagRepository.save(tag2);
        tagRepository.save(tag3);
    }
}
