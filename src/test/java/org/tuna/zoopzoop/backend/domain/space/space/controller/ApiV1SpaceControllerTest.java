package org.tuna.zoopzoop.backend.domain.space.space.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.tuna.zoopzoop.backend.testSupport.ControllerTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1SpaceControllerTest extends ControllerTestSupport {

    // ============================= CREATE ============================= //

    @Test
    @DisplayName("스페이스 생성 - 성공")
    void createSpace_Success() {


    }


    // ======================= TEST DATA FACTORIES ======================== //

//    private ClubCreateRequestDto createDefaultClubCreateDto() {
//        return new ClubCreateRequestDto(
//                "테스트 그룹",
//                "테스트 그룹 설명",
//                ClubCategory.TRAVEL,
//                "서울",
//                10,
//                EventType.SHORT_TERM,
//                LocalDate.of(2023, 10, 1),
//                LocalDate.of(2023, 10, 31),
//                true,
//                List.of()
//        );
//    }


}