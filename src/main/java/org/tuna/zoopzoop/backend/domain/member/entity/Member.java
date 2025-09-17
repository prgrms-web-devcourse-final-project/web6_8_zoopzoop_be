package org.tuna.zoopzoop.backend.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tuna.zoopzoop.backend.global.jpa.entity.BaseEntity;

@Setter
@Getter
@Entity
@NoArgsConstructor
public class Member extends BaseEntity {
    //사용자 이름
    //UNIQUE 해야 하나?
    @Column(unique = true, nullable = false)
    private String username;

    //사용자 이메일
    //검색 조건으로 사용할 것이므로, UNIQUE 해야함.
    @Column(unique = true, nullable = false)
    private String email;

    //사용자 프로필 이미지 URL
    @Column
    private String profileImageUrl;

    /**
     * Member 엔티티 빌더
     *
     * @param username 사용자 이름
     * @param email 사용자 이메일
     * @param profileImageUrl 사용자 프로필 이미지 URL
     */
    //이런 형식으로 작성해주시면 됩니다.
    //물론 지금처럼 코드가 직관적인 경우에는, 굳이 작성 하실 필요 없습니다.
    //해당 방식으로 작성하실 경우, 도구 -> javadoc 생성을 통해 자동 문서화가 가능합니다.
    //추가로 @return과 같은 어노테이션도 사용이 가능합니다.
    @Builder
    public Member(String username, String email, String profileImageUrl) {
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }
}
