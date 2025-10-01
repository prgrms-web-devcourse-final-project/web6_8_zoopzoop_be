package org.tuna.zoopzoop.backend.domain.dashboard.extraComponent;

import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.GraphUpdateMessage;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.testSupport.ControllerTestSupport;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphUpdateConsumerTest extends ControllerTestSupport {
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired SpaceService spaceService;

    // 테스트에 사용할 dashboardId (실제 DB에 존재하는 ID)
    private Integer existingDashboardId;

    private String existingSpaceName = "TestSpace1_forGraphUpdateConsumerTest";

    @BeforeAll
    void setUp(){
        spaceService.createSpace(existingSpaceName, "thumb1");
    }


    @Test
    @DisplayName("큐에 업데이트 메시지가 들어오면 컨슈머가 DB를 성공적으로 업데이트한다")
    void handleGraphUpdate_Success() throws Exception {
        // Given
        // ControllerTestSupport의 setUp에서 생성된 대시보드 ID를 가져옵니다.
        existingDashboardId = spaceService.findByName(existingSpaceName).getDashboard().getId();

        String requestBody = createReactFlowJsonBody(); // 테스트용 JSON 데이터
        GraphUpdateMessage message = new GraphUpdateMessage(existingDashboardId, requestBody);

        // When: 테스트에서 직접 RabbitMQ에 메시지를 발행합니다.
        rabbitTemplate.convertAndSend("zoopzoop.exchange", "graph.update.rk", message);

        // Then: 컨슈머가 메시지를 처리하여 DB가 변경될 때까지 최대 5초간 기다립니다.
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            transactionTemplate.execute(status -> {
                Space space = spaceService.findByName(existingSpaceName);
                Graph updatedGraph = space.getDashboard().getGraph();
                assertThat(updatedGraph.getNodes()).hasSize(2);
                assertThat(updatedGraph.getEdges()).hasSize(1);
                assertThat(updatedGraph.getNodes().get(0).getData().get("title")).isEqualTo("노드1");
                return null;
            });
        });
    }

    private String createReactFlowJsonBody() {
        return """
            {
                "nodes": [
                    {
                        "id": "1",
                        "type": "CUSTOM",
                        "data": { "title": "노드1", "description": "설명1" },
                        "position": { "x": 100, "y": 200 }
                    },
                    {
                        "id": "2",
                        "type": "CUSTOM",
                        "data": { "title": "노드2" },
                        "position": { "x": 300, "y": 400 }
                    }
                ],
                "edges": [
                    {
                        "id": "e1-2",
                        "source": "1",
                        "target": "2",
                        "type": "SMOOTHSTEP",
                        "animated": true,
                        "style": { "stroke": "#999", "strokeWidth": 2.0 }
                    }
                ]
            }
            """;
    }

}