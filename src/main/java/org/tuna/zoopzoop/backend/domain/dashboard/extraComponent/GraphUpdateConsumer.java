package org.tuna.zoopzoop.backend.domain.dashboard.extraComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.BodyForReactFlow;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.GraphUpdateMessage;
import org.tuna.zoopzoop.backend.domain.dashboard.service.DashboardService;

@Slf4j
@Component
@RequiredArgsConstructor
public class GraphUpdateConsumer {
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "graph.update.queue")
    public void handleGraphUpdate(GraphUpdateMessage message) {
        log.info("Received graph update message for dashboardId: {}", message.dashboardId());
        try {
            BodyForReactFlow dto = objectMapper.readValue(message.requestBody(), BodyForReactFlow.class);
            dashboardService.updateGraph(message.dashboardId(), dto);
            log.info("Successfully updated graph for dashboardId: {}", message.dashboardId());
        } catch (ObjectOptimisticLockingFailureException e) {
            // Optimistic Lock 충돌 발생!
            // 내가 처리하려던 메시지는 이미 구버전 데이터에 대한 요청이었음.
            // 따라서 이 메시지는 무시하고 정상 처리된 것으로 간주.
            log.warn("Stale update attempt for dashboardId: {}. A newer version already exists. Discarding message.", message.dashboardId());
            // 예외를 다시 던지지 않으므로, 메시지는 큐에서 정상적으로 제거(ACK)됩니다.
        } catch (Exception e) {
            // 실제 운영에서는 메시지를 재시도하거나, 실패 큐(Dead Letter Queue)로 보내는 등의
            // 정교한 에러 처리 로직이 필요합니다.
            log.error("Failed to process graph update for dashboardId: {}", message.dashboardId(), e);
        }
    }
}
