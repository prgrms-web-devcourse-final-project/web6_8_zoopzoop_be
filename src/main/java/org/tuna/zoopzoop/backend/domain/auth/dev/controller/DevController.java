package org.tuna.zoopzoop.backend.domain.auth.dev.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.tuna.zoopzoop.backend.domain.member.entity.Member;
import org.tuna.zoopzoop.backend.domain.member.enums.Provider;
import org.tuna.zoopzoop.backend.domain.member.repository.MemberRepository;
import org.tuna.zoopzoop.backend.global.security.jwt.JwtUtil;

import java.util.Map;

@Profile({"local","dev","staging","test"})
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevController {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/token")
    public Map<String, String> issueToken(
            @RequestParam Provider provider,
            @RequestParam String key
    ) {
        Member m = memberRepository.findByProviderAndProviderKey(provider, key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found"));

        String accessToken = jwtUtil.generateToken(m);
        return Map.of("accessToken", accessToken);
    }
}
