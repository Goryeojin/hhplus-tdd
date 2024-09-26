package io.hhplus.tdd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * E2E 통합 테스트
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class TddApplicationTest {
    @Autowired
    private MockMvc mvc;

    static final long USER_ID = 1L;
    static final long AMOUNT = 100L;

    @Test
    @DisplayName("유저 포인트 충전 요청 시 성공적으로 처리된다.")
    void chargeUserPointsSuccessfully() throws Exception {
        mvc.perform(patch("/point/{id}/charge", USER_ID) // 검증할 uri 호출
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(AMOUNT)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk()) // 결과가 성공인지 검증
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(USER_ID)) // 예상하는 id와 일치하는지 검증
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(AMOUNT)); // 예상하는 point와 일치하는지 검증
    }

    @Test
    @DisplayName("유저 포인트 사용 요청 시 성공적으로 처리된다.")
    void useUserPointsSuccessfully() throws Exception {
        // 충전 후 사용 테스트
        mvc.perform(patch("/point/{id}/charge", USER_ID) // 포인트 정상 사용을 위한 충전 진행
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(AMOUNT)));

        mvc.perform(patch("/point/{id}/use", USER_ID) // 검증할 uri 호출
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.valueOf(50L)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk()) // 결과가 성공인지 검증
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(USER_ID))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(50L));
    }

    @Test
    @DisplayName("조회할 유저 아이디가 숫자가 아닌 경우 조회에 실패한다")
    void failToReturnPointIfUserIdIsNotNumber() throws Exception {
        mvc.perform(get("/point/{id}", "CHAR")) // 검증할 uri 호출
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // 결과가 400 BAD REQUEST 인지 검증
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(containsString("잘못된 요청 값입니다.")));
    }

    @Test
    @DisplayName("조회 가능한 유저의 포인트를 조회한다.")
    void returnUserPointViewableUsePoint() throws Exception {
        mvc.perform(get("/point/{id}", USER_ID)) // 검증할 uri 호출
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk()) // 결과가 성공인지 검증
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(USER_ID))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(0L));
    }

    @Test
    @DisplayName("유저 포인트 사용 금액이 잔액 보다 클 경우 사용에 실패한다.")
    void failToUsePointsWhenAmountExceedsBalance() throws Exception {
        // 초기 포인트가 0인 상태에서 사용 요청
        mvc.perform(patch("/point/{id}/use", USER_ID) // 검증할 uri 호출
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(AMOUNT)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // 결과가 400 BAD REQUEST 인지 검증
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("포인트 잔액이 부족합니다. 잔액: 0"));
    }

    @Test
    @DisplayName("유저 포인트 사용하려는 금액이 0 이하일 경우 사용에 실패한다.")
    void failToUsePointsWhenAmountIsZeroOrNegative() throws Exception {
        // 충전 후 사용 테스트
        mvc.perform(patch("/point/{id}/charge", USER_ID) // 포인트 사용을 위한 충전 진행
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(AMOUNT)));

        // 사용 금액이 0인 경우
        mvc.perform(patch("/point/{id}/use", USER_ID) // 검증할 uri 호출
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(0)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // 결과가 400 BAD REQUEST 인지 검증
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("사용할 포인트는 0보다 커야 합니다."));

        // 사용 금액이 음수인 경우
        mvc.perform(patch("/point/{id}/use", USER_ID) // 검증할 uri 호출
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(-10)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // 결과가 400 BAD REQUEST 인지 검증
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("사용할 포인트는 0보다 커야 합니다."));
    }

    @Test
    @DisplayName("유저 충전 금액이 충전 후 합산 금액이 최대 잔고를 초과하면 충전에 실패한다.")
    void failToChargeWhenExceedingMaxBalance() throws Exception {
        // 최대 잔고에 도달한 상태에서 충전 요청
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(1_000_000L))); // 최대 잔고 충전

        mvc.perform(patch("/point/{id}/charge", USER_ID) // 검증할 uri 호출
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(1)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // 결과가 400 BAD REQUEST 인지 검증
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("포인트 최대 잔고는 1000000포인트 입니다. 잔액: 1000000"));
    }

    @Test
    @DisplayName("유저 충전 금액이 0 이하인 경우 충전에 실패한다.")
    void failToChargeWhenAmountIsZeroOrNegative() throws Exception {
        // 충전 금액이 0인 경우
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(0)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("충전할 포인트는 0보다 커야 합니다."));

        // 충전 금액이 음수인 경우
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(-100)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("충전할 포인트는 0보다 커야 합니다."));
    }

    @Test
    @DisplayName("유저의 포인트 충전/사용 내역을 조회할 수 있다.")
    void returnPointHistory() throws Exception {
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(AMOUNT)));

        mvc.perform(patch("/point/{id}/use", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(AMOUNT)));

        mvc.perform(patch("/point/{id}/charge", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(AMOUNT)));

        mvc.perform(get("/point/{id}/histories", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].type").value("CHARGE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].amount").value(AMOUNT))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].type").value("USE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].amount").value(AMOUNT))
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].type").value("CHARGE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].amount").value(AMOUNT));

    }
}
