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
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
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

        aiService.summarizeAndTag("안녕 내 이름은 오수혁이야");
        aiService.summarizeAndTag("D2Coding 1.3.2 버전을 릴리즈 합니다. ligature 관련 이슈를 수정하여, ligature 적용/미적용 폰트를 구분하여 배포합니다.\n" +
                "\n" +
                "기존 버전은 반드시 삭제후 설치 바랍니다.\n" +
                "\n");

        aiService.summarizeAndTag("Spring AI는 예외 발생 시 AiClientException, RetryableException 등으로 예외를 포장합니다. Spring의 기본 예외 처리 방식을 활용하면 에러 핸들링을 일관성 있게 구성할 수 있습니다.");
    }
}
