package org.tuna.zoopzoop.backend.domain.dashboard.extraComponent;

import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.GraphUpdateMessage;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Edge;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Graph;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Node;
import org.tuna.zoopzoop.backend.domain.space.space.entity.Space;
import org.tuna.zoopzoop.backend.domain.space.space.service.SpaceService;
import org.tuna.zoopzoop.backend.testSupport.ControllerTestSupport;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphUpdateConsumerTest extends ControllerTestSupport {
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private SpaceService spaceService;

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

                // 1. 노드와 엣지의 전체 개수 검증
                assertThat(updatedGraph.getNodes()).hasSize(2);
                assertThat(updatedGraph.getEdges()).hasSize(1);

                // 2. 특정 노드(id="1")의 상세 데이터 검증
                // DTO의 List 순서대로 엔티티가 생성되므로 get(0)으로 첫 번째 노드를 특정합니다.
                Node firstNode = updatedGraph.getNodes().get(0);
                assertThat(firstNode.getNodeKey()).isEqualTo("1");
                assertThat(firstNode.getPositionX()).isEqualTo(100.0);
                assertThat(firstNode.getPositionY()).isEqualTo(200.0);

                // Node의 data Map 내부 값들을 상세히 검증
                Map<String, String> data = firstNode.getData();
                assertThat(data.get("title")).isEqualTo("노드 1");
                assertThat(data.get("content")).isEqualTo("첫 번째 노드에 대한 간단한 요약 내용입니다. 이 내용은 노드 내부에 표시됩니다.");
                assertThat(data.get("sourceUrl")).isEqualTo("https://example.com/source1");
                assertThat(data.get("writerName")).isEqualTo("김Tuna");
                assertThat(data.get("writerProfileImageUrl")).isEqualTo("https://example.com/profiles/tuna.jpg");

                // 3. 엣지(id="e1-2")의 상세 데이터 검증
                Edge edge = updatedGraph.getEdges().get(0);
                assertThat(edge.getEdgeKey()).isEqualTo("e1-2");
                assertThat(edge.getSourceNodeKey()).isEqualTo("1");
                assertThat(edge.getTargetNodeKey()).isEqualTo("2");

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
                            "selected": false,
                            "dragging": false,
                            "position": {
                                "x": 100,
                                "y": 200
                            },
                            "measured": {
                                "width": 250,
                                "height": 150
                            },
                            "data": {
                                "content": "첫 번째 노드에 대한 간단한 요약 내용입니다. 이 내용은 노드 내부에 표시됩니다.",
                                "createdAt": "2025-10-02",
                                "link": "https://example.com/source1",
                                "title": "노드 1",
                                "user": {
                                    "name": "김Tuna",
                                    "profileUrl": "https://example.com/profiles/tuna.jpg"
                                }
                            }
                        },
                        {
                            "id": "2",
                            "type": "CUSTOM",
                            "selected": false,
                            "dragging": false,
                            "position": {
                                "x": 500,
                                "y": 300
                            },
                            "measured": {
                                "width": 250,
                                "height": 150
                            },
                            "data": {
                                "content": "두 번째 노드에 대한 요약입니다. 원본 소스는 다른 곳을 가리킵니다.",
                                "createdAt": "2025-10-01",
                                "link": "https://example.com/source2",
                                "title": "노드 2",
                                "user": {
                                    "name": "박Zoop",
                                    "profileUrl": "https://example.com/profiles/zoop.jpg"
                                }
                            }
                        }
                    ],
                    "edges": [
                        {
                            "id": "e1-2",
                            "source": "1",
                            "target": "2"
                        }
                    ]
                }
            """;
    }

}