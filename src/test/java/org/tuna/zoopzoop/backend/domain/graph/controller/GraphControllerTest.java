package org.tuna.zoopzoop.backend.domain.graph.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.domain.graph.entity.Graph;
import org.tuna.zoopzoop.backend.domain.graph.repository.GraphRepository;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class GraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GraphRepository graphRepository;

    @BeforeEach
    void setUp() {
        graphRepository.deleteAll(); // 테스트 전 DB 초기화
    }

    // 단위 테스트가 백엔드 컨벡션 원칙이나, 서비스 특성 상 단위 테스트가 어려워
    // 일단 통합 테스트로 진행합니다.
    @Test
    @DisplayName("React-flow 데이터 저장 및 조회 테스트 - JSON 방식")
    void createAndGetGraphTest() throws Exception {
        // 테스트 용 React-flow JSON
        String jsonBody = """
            {
                "nodes": [
                    {
                        "id": "1",
                        "type": "custom",
                        "data": {
                            "title": "노드1",
                            "description": "설명1"
                        },
                        "position": {
                            "x": 100,
                            "y": 200
                        }
                    },
                    {
                        "id": "2",
                        "type": "custom",
                        "data": {
                            "title": "노드2"
                        },
                        "position": {
                            "x": 300,
                            "y": 400
                        }
                    }
                ],
                "edges": [
                    {
                        "id": "e1-2",
                        "source": "1",
                        "target": "2",
                        "type": "smoothstep",
                        "animated": true,
                        "style": {
                            "stroke": "#999",
                            "strokeWidth": 2.0
                        }
                    }
                ]
            }
            """;

        // React-flow 데이터 저장
        mockMvc.perform(post("/api/v1/graph")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("React-flow 데이터를 저장 했습니다."))
                .andExpect(jsonPath("$.data").isEmpty());

        // DB 무결성 검증
        assertEquals(1, graphRepository.count());
        Graph savedGraph = graphRepository.findAll().get(0);
        assertEquals(2, savedGraph.getNodes().size());
        assertEquals(1, savedGraph.getEdges().size());

        // 저장된 React-flow 데이터 조회
        mockMvc.perform(get("/api/v1/graph/{id}", savedGraph.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.msg").value("ID: " + savedGraph.getId() + " 의 React-flow 데이터를 조회했습니다."))
                .andExpect(jsonPath("$.data.nodes", hasSize(2)))
                .andExpect(jsonPath("$.data.edges", hasSize(1)))
                .andExpect(jsonPath("$.data.nodes[0].id").value("1"))
                .andExpect(jsonPath("$.data.nodes[0].type").value("custom"))
                .andExpect(jsonPath("$.data.nodes[0].data.title").value("노드1"))
                .andExpect(jsonPath("$.data.edges[0].id").value("e1-2"))
                .andExpect(jsonPath("$.data.edges[0].animated").value(true));
    }
}