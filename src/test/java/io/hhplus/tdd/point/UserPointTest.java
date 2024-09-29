package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class UserPointTest {
    static final Long USER_ID = 1L; // 테스트에 쓰일 유저 ID

    // PointUser charge() 예외처리 테스트
    @Test
    @DisplayName("충전 포인트가 0 이하일 경우 IllegalArgumentException이 발생한다.")
    void chargePointShouldThrowExceptionWhenAmountIsZeroOrNegative() {
        //given
        // 초기 유저 포인트 객체
        UserPoint userPoint = UserPoint.empty(USER_ID);

        //when-then
        assertThatThrownBy(() -> userPoint.charge(0)) // 검증할 메서드 실행
                .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
                .hasMessage("충전할 포인트는 0보다 커야 합니다.");

        assertThatThrownBy(() -> userPoint.charge(-100)) // 검증할 메서드 실행
                .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
                .hasMessage("충전할 포인트는 0보다 커야 합니다.");
    }

    // PointUser charge() 예외 처리 테스트
    @Test
    @DisplayName("충전 최대 잔고를 초과할 경우 IllegalArgumentException이 발생한다.")
    void chargePointShouldThrowExceptionWhenExceedingMaxBalance() {
        //given
        // 초기 유저 포인트 객체 (최대 잔고에 딱 맞게 설정)
        UserPoint userPoint = new UserPoint(USER_ID, 1_000_000L, System.currentTimeMillis());

        //when-then
        assertThatThrownBy(() -> userPoint.charge(1)) // 검증할 메서드 실행
                .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
                .hasMessage("포인트 최대 잔고는 1000000포인트 입니다. 잔액: 1000000");
    }

    // PointUser charge() 성공 테스트
    @Test
    @DisplayName("충전 성공 시 포인트가 증가한다.")
    void chargePointShouldIncreasePoints() {
        //given
        // 초기 유저 포인트 객체
        UserPoint userPoint = UserPoint.empty(USER_ID);

        //when
        UserPoint updatedPoint = userPoint.charge(100); // 검증할 메서드 실행

        //then
        assertThat(updatedPoint.point()).isEqualTo(100); // 충전 후 포인트가 예상한 값과 일지하는지 검증
    }

    // PointUser use() 예외처리 테스트
    @Test
    @DisplayName("사용 포인트가 0 이하일 경우 IllegalArgumentException이 발생한다.")
    void usePointShouldThrowExceptionWhenAmountIsZeroOrNegative() {
        //given
        // 초기 유저 포인트 객체를 생성하고 충전을 미리 진행한다.
        UserPoint userPoint = UserPoint.empty(USER_ID).charge(100L);

        //when-then
        assertThatThrownBy(() -> userPoint.use(0L)) // 검증할 메서드 실행
                .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
                .hasMessage("사용할 포인트는 0보다 커야 합니다.");

        assertThatThrownBy(() -> userPoint.use(-50L)) // 검증할 메서드 실행
                .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
                .hasMessage("사용할 포인트는 0보다 커야 합니다.");
    }

    // PointUser use() 예외처리 테스트
    @Test
    @DisplayName("잔액보다 사용 포인트가 많을 경우 IllegalArgumentException이 발생한다.")
    void usePointShouldThrowExceptionWhenInsufficientBalance() {
        //given
        // 초기 유저 포인트 객체를 생성하고 충전을 미리 진행한다.
        UserPoint userPoint = UserPoint.empty(USER_ID).charge(100L);

        //when-then
        assertThatThrownBy(() -> userPoint.use(200L)) // 검증할 메서드 실행
                .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
                .hasMessage("포인트 잔액이 부족합니다. 잔액: 100");
    }

    // PointUser use() 성공 테스트
    @Test
    @DisplayName("사용 성공 시 포인트가 감소한다.")
    void usePointShouldDecreasePoints() {
        //given
        // 초기 유저 포인트 객체를 생성하고 충전을 미리 진행한다.
        UserPoint userPoint = UserPoint.empty(USER_ID).charge(100);

        //when
        UserPoint updatedPoint = userPoint.use(50L); // 검증할 메서드 실행

        //then
        assertThat(updatedPoint.point()).isEqualTo(50); // 사용한 만큼 포인트가 차감되었는지 검증
    }

}
