package org.tuna.zoopzoop.backend.domain.space.membership.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.SSE.service.EmitterService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private EmitterService emitterService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("초대 알림 전송 시 EmitterService의 sendNotification이 올바르게 호출됨")
    void sendSpaceInvitation_CallsEmitterService() {
        // given (준비)
        Long memberId = 1L;
        String testData = "test data";

        // when (실행)
        notificationService.sendSpaceInvitation(memberId, testData);

        // then (검증)
        // EmitterService의 sendNotification 메소드가
        // memberId, "space-invitation", testData 파라미터로
        // 1번 호출되었는지 검증
        verify(emitterService, times(1)).sendNotification(memberId, "space-invitation", testData);
    }

}