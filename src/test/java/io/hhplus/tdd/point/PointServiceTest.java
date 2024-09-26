package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    @InjectMocks
    PointService pointService; // 테스트 대상 객체 주입

    /*
     * UserPointTable, PointHistoryTable 은 이미 검증된 클래스라고 가정한다.
     * @Mock : Mock 객체로 해당 클래스의 의존성을 주입하여, 비즈니스 로직만 테스트한다.
     */
    @Mock
    UserPointRepository userPointRepository;
    @Mock
    PointHistoryRepository pointHistoryRepository;

    static final Long USER_ID = 1L; // 테스트에 쓰일 유저 ID

    @Test
    @DisplayName("충전하려는 포인트가 0원 이하일 경우 충전에 실패한다.")
    void failToChargeIfAmountIsNotGreaterThanZero() {
        //given
        long chargeAmount = 0L; // 충전할 금액
        UserPoint userPoint = new UserPoint(USER_ID, chargeAmount, System.currentTimeMillis()); // 초기 유저 포인트 객체

        // 유저의 현재 포인트 조회 시 mock 객체 반환 설정
        given(userPointRepository.findById(USER_ID)).willReturn(userPoint);

        //when - then
        assertThatThrownBy(() -> {
            pointService.charge(USER_ID, chargeAmount); // 검증할 메서드 실행
        })
            .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
            .hasMessageContaining("0보다 커야 합니다.");
    }

    @Test
    @DisplayName("최대 잔고에 초과되지 않게, 0원이 넘는 포인트를 충전하면 충전된다.")
    void chargePointWhenAmountIsGreaterThanZero() {
        //given
        long chargeAmount = 1_000L; // 충전할 금액
        long expectedAmount = 2_000L; // 예상되는 최종 포인트
        UserPoint userPoint = new UserPoint(USER_ID, chargeAmount, System.currentTimeMillis()); // 초기 유저 포인트 객체

        // 유저의 현재 포인트 조회 시 mock 객체 반환 설정
        given(userPointRepository.findById(USER_ID)).willReturn(userPoint);
        // 포인트 충전 후 예상되는 유저 포인트 객체 반환 설정
        given(userPointRepository.saveOrUpdate(USER_ID, expectedAmount))
                .willReturn(new UserPoint(USER_ID, expectedAmount, System.currentTimeMillis()));

        //when
        UserPoint result = pointService.charge(USER_ID, chargeAmount); // 검증할 메서드 실행

        //then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(expectedAmount); // 결과 포인트가 예상 금액과 일치하는지 검증
        // 부수적인 동작인 포인트 충전 기록을 남기는지 검증
        verify(pointHistoryRepository).save(eq(USER_ID), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    // 최대 잔고를 넘으면 실패
    @Test
    @DisplayName("충전 시 최대 잔고를 넘으면 충전에 실패한다.")
    void failToChargeIfChargePointIsGreaterThanMaxPoint() {
        //given
        long chargeAmount = 1_000_001L; // 충전할 금액
        // 최대 금액 1_000_000L

        UserPoint userPoint = new UserPoint(USER_ID, 0L, System.currentTimeMillis()); // 초기 유저 포인트 객체

        // 유저의 현재 포인트 조회 시 mock 객체 반환 설정
        given(userPointRepository.findById(USER_ID)).willReturn(userPoint);

        //when - then
        assertThatThrownBy(() -> {
            pointService.charge(USER_ID, chargeAmount); // 검증할 메서드 실행
        })
                .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
                .hasMessageContaining("포인트 최대 잔고는");
    }
  
    @Test
    @DisplayName("특정 유저의 포인트 정보를 리턴한다.")
    void returnUsePointInfoWhenUserExists() {
        //given
        UserPoint expected = new UserPoint(USER_ID, 0L, 0L); // 예상되는 유저 포인트 객체
        given(userPointRepository.findById(USER_ID)).willReturn(expected); // 유저의 현재 포인트 조회 시 mock 객체 반환 설정

        //when
        UserPoint result = pointService.findPoint(USER_ID); // 검증할 메서드 실행

        //then
        assertThat(result.id()).isEqualTo(USER_ID); // 결과 유저 ID가 예상과 일치하는지 검증
        verify(userPointRepository).findById(eq(USER_ID)); // 해당 메서드가 호출되었는지 검증
    }

    @Test
    @DisplayName("사용하려는 포인트가 0원 이하일 경우 사용에 실패한다.")
    void failToUseIfUseAmountIsNotGreaterThanZero() {
        //given
        long useAmount = 0L;
        UserPoint userPoint = new UserPoint(USER_ID, useAmount, System.currentTimeMillis()); // 초기 유저 포인트 객체

        // 유저의 현재 포인트 조회 시 mock 객체 반환 설정
        given(userPointRepository.findById(USER_ID)).willReturn(userPoint);

        //when - then
        assertThatThrownBy(() -> {
            pointService.use(USER_ID, useAmount); // 검증할 메서드 실행
        })
                .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
                .hasMessageContaining("0보다 커야 합니다.");
    }

    @Test
    @DisplayName("사용하려는 포인트가 잔액보다 클 경우 사용에 실패한다.")
    void failToUseIfUseAmountIsGreaterThanGivenPoint() {
        //given
        long useAmount = 10_000L;
        UserPoint userPoint = new UserPoint(USER_ID, 1_000L, System.currentTimeMillis()); // 초기 유저 포인트 객체

        // 유저의 현재 포인트 조회 시 mock 객체 반환 설정
        given(userPointRepository.findById(USER_ID)).willReturn(userPoint);

        //when - then
        assertThatThrownBy(() -> {
            pointService.use(USER_ID, useAmount); // 검증할 메서드 실행
        })
                .isInstanceOf(IllegalArgumentException.class) // IllegalArgumentException 예외를 발생시키는지 검증
                .hasMessageContaining("잔액이 부족합니다.");
    }

    @Test
    @DisplayName("유저의 남은 포인트 내에서 포인트를 사용한다.")
    void usePointWhenGivenUserIdAndAmount() {
        //given
        long chargeAmount = 10_000L; // 초기 충전 금액
        long useAmount = 1_000L; // 사용할 금액
        long expectedAmount = 9_000L; // 예상되는 최종 포인트
        UserPoint userPoint = new UserPoint(USER_ID, chargeAmount, System.currentTimeMillis()); // 초기 유저 포인트 객체

        // 유저의 현재 포인트 조회 시 mock 객체 반환 설정
        given(userPointRepository.findById(USER_ID)).willReturn(userPoint);
        // 포인트 충전 후 예상되는 유저 포인트 객체 반환 설정
        given(userPointRepository.saveOrUpdate(USER_ID, expectedAmount))
                .willReturn(new UserPoint(USER_ID, expectedAmount, System.currentTimeMillis()));

        //when
        UserPoint result = pointService.use(userPoint.id(), useAmount); // 검증할 메서드 실행

        //then
        assertThat(result.point()).isEqualTo(expectedAmount); // 결과 포인트가 예상 금액과 일치하는지 검증
        // 포인트 사용 기록을 남기는지 검증
        verify(pointHistoryRepository).save(eq(USER_ID), eq(useAmount), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전/사용 내역을 조회한다.")
    void returnUserPointHistoryWhenUseExists() {
        //given
        // 예상 포인트 내역 설정
        List<PointHistory> expectedList = List.of(
                new PointHistory(1L, USER_ID, 1_000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, USER_ID, 500L, TransactionType.USE, System.currentTimeMillis())
        );
        // 유저의 포인트 내역 조회 시 mock 객체 반환 설정
        given(pointHistoryRepository.findAllById(USER_ID)).willReturn(expectedList);

        //when
        List<PointHistory> result = pointService.findHistory(USER_ID); // 검증할 메서드 실행

        //then
        assertThat(result)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("updateMillis")
                .isEqualTo(expectedList); // updateMillis 제외하고 비교
        // 포인트 내역 조회 메서드가 호출되었는지 검증
        verify(pointHistoryRepository).findAllById(eq(USER_ID));
    }
}