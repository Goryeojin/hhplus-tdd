package io.hhplus.tdd.point;

import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest 애노테이션을 사용해 Spring MVC 를 테스트할 수 있다.
 *
 */
@WebMvcTest(PointController.class)
class PointControllerTest {

    static final long USER_ID = 1L;
    static final long CHARGE_AMOUNT = 1_000L;
    static final long USE_AMOUNT = 200L;
    MockMvc mvc;

    @MockBean
    PointService pointService;
    @Autowired
    WebApplicationContext ctx;

    @BeforeEach
    public void setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }


    @Test
    @DisplayName("특정 유저의 포인트 조회")
    void getUserPoint() throws Exception {
        //given
        UserPoint userPoint = new UserPoint(USER_ID, 0L, 0L);
        given(pointService.findPoint(USER_ID)).willReturn(userPoint);

        //when
        //then
        mvc.perform(get("/point/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(0L))
                .andExpect(jsonPath("$.updateMillis").value(0L));
        verify(pointService).findPoint(USER_ID);
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전/사용 내역 조회")
    void getUserPointHistory() throws Exception {
        //given
        List<PointHistory> histories = List.of(
                new PointHistory(1L, USER_ID, CHARGE_AMOUNT, TransactionType.CHARGE, 0L),
                new PointHistory(1L, USER_ID, USE_AMOUNT, TransactionType.USE, 0L)
        );
        given(pointService.getUserPointHistory(USER_ID)).willReturn(histories);

        //when
        //then
        mvc.perform(get("/point/{id}/histories", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[0].amount").value(CHARGE_AMOUNT))
                .andExpect(jsonPath("$.[1].amount").value(USE_AMOUNT));
        verify(pointService).getUserPointHistory(USER_ID);
    }

    @Test
    @DisplayName("유저의 포인트 충전 요청")
    void chargeUserPoint() throws Exception {
        //given
        UserPoint userPoint = new UserPoint(USER_ID, CHARGE_AMOUNT, 0);
        given(pointService.chargeUserPoint(USER_ID, CHARGE_AMOUNT)).willReturn(userPoint);

        //when
        //then
        performPatch("/point/{id}/charge", CHARGE_AMOUNT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(CHARGE_AMOUNT));
    }

    @Test
    @DisplayName("유저의 포인트 사용 요청")
    void useUserPoint() throws Exception {
        //given
        UserPoint userPoint = new UserPoint(USER_ID, CHARGE_AMOUNT - USE_AMOUNT, 0);
        given(pointService.useUserPoint(USER_ID, USE_AMOUNT)).willReturn(userPoint);

        //when
        //then
        performPatch("/point/{id}/use", USE_AMOUNT)
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(CHARGE_AMOUNT - USE_AMOUNT));
    }

    private ResultActions performPatch(String uri, long amount) throws Exception {
        return mvc.perform(patch(uri, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(amount)));
    }
}