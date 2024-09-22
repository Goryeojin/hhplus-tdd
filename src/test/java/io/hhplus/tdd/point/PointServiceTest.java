package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PointServiceTest {

    PointService pointService;
    UserPointTable userPointTable;
    PointHistoryTable pointHistoryTable;

    @BeforeEach
    void beforeEach() {
        /*
         * 데이터를 DB 저장이 아닌 메모리에 저장하기에 Mock 객체 사용하지 않는다.
         * 테스트 시 데이터 중복 처리를 방지하기 위해
         * 매 테스트 시행 시 Service 와 Repository 객체를 새로 생성한다.
         */
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("특정 유저가 포인트를 충전하면 충전된 포인트 객체를 반환한다")
    void chargeReturnsPoints() {
        //given
        long userId = 1L;
        long chargeAmount = 1_000L;
        long expectedAmount = 2_000L;

        //when
        pointService.chargeUserPoint(userId, chargeAmount);
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount);

        //then
        assertThat(result.point()).isEqualTo(expectedAmount);
    }
  
    @Test
    @DisplayName("특정 유저의 포인트를 조회한다.")
    void getUserPoint() {
        //given
        long userId = 1L;

        //when
        UserPoint result = pointService.getUserPoint(userId);

        //then
        assertThat(result.id()).isEqualTo(userId);
    }
}