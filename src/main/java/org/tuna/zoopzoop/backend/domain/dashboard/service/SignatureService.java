package org.tuna.zoopzoop.backend.domain.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class SignatureService {
    @Value("${liveblocks.secret-key}")
    private String liveblocksSecretKey;

    // 5분 (밀리초 단위)
    private static final long TOLERANCE_IN_MILLIS = 5 * 60 * 1000;

    /**
     * LiveBlocks Webhook 요청의 유효성을 검증하는 메서드
     * @param requestBody 요청 바디
     * @param signatureHeader LiveBlocks가 제공하는 서명 헤더
     * @return 서명이 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidSignature(String requestBody, String signatureHeader) {
        // [임시 코드] 로컬 테스트를 위해 무조건 true 반환
//        if ("true".equals(System.getProperty("local.test.skip.signature"))) {
//            return true;
//        }

        try {
            // 1. 헤더 파싱
            String[] parts = signatureHeader.split(",");
            long timestamp = -1;
            String signatureHashFromHeader = null;

            for (String part : parts) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2) {
                    if ("t".equals(pair[0])) {
                        timestamp = Long.parseLong(pair[1]);
                    } else if ("v1".equals(pair[0])) {
                        signatureHashFromHeader = pair[1];
                    }
                }
            }

            if (timestamp == -1 || signatureHashFromHeader == null) {
                return false; // 헤더 형식이 잘못됨
            }

            // 2. 리플레이 공격 방지를 위한 타임스탬프 검증 (선택사항)
            long now = System.currentTimeMillis();
            if (now - timestamp > TOLERANCE_IN_MILLIS) {
                return false; // 너무 오래된 요청
            }

            // 3. 서명 재생성
            String payload = timestamp + "." + requestBody;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(liveblocksSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] expectedHashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // 4. 서명 비교 (타이밍 공격 방지를 위해 MessageDigest.isEqual 사용)
            byte[] signatureHashBytesFromHeader = Hex.decodeHex(signatureHashFromHeader);
            return MessageDigest.isEqual(expectedHashBytes, signatureHashBytesFromHeader);

        } catch (Exception e) {
            // 파싱 실패, 디코딩 실패 등 모든 예외는 검증 실패로 간주
            return false;
        }
    }
}
