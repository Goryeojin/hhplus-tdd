package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PointServiceTest {

    PointService pointService;
    UserPointTable userPointTable;

    @BeforeEach
    void beforeEach() {
        userPointTable = new UserPointTable();
        pointService = new PointService(userPointTable);
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