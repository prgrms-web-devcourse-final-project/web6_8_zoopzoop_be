package org.tuna.zoopzoop.backend.domain.space.membership.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.SSE.service.EmitterService;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final EmitterService emitterService;

    @Async // 별도 스레드에서 비동기 실행
    public void sendSpaceInvitation(Long memberId, Object invitationData) {
        emitterService.sendNotification(memberId, "space-invitation", invitationData);
    }
}
