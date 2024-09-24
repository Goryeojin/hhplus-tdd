package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("특정 유저가 포인트를 충전하면 금액만큼 충전하여 객체를 반환한다")
    void chargePoint() {
        //given
        long userId = USER_ID;
        long chargeAmount = 1_000L; // 충전할 금액
        long expectedAmount = 2_000L; // 예상되는 최종 포인트
        UserPoint userPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis()); // 초기 유저 포인트 객체

        // 유저의 현재 포인트 조회 시 mock 객체 반환 설정
        given(userPointRepository.selectById(userId)).willReturn(userPoint);
        // 포인트 충전 후 예상되는 유저 포인트 객체 반환 설정
        given(userPointRepository.insertOrUpdate(userId, expectedAmount))
                .willReturn(new UserPoint(userId, expectedAmount, System.currentTimeMillis()));

        //when
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount); // 검증할 메서드 실행

        //then
        assertThat(result.point()).isEqualTo(expectedAmount); // 결과 포인트가 예상 금액과 일치하는지 검증
        // 부수적인 동작인 포인트 충전 기록을 남기는지 검증
        verify(pointHistoryRepository).insert(eq(userId), eq(expectedAmount), eq(TransactionType.CHARGE), anyLong());
    }
  
    @Test
    @DisplayName("특정 유저의 포인트를 조회한다.")
    void getUserPoint() {
        //given
        long userId = USER_ID;
        UserPoint expected = new UserPoint(userId, 0L, System.currentTimeMillis()); // 예상되는 유저 포인트 객체

        // 유저의 현재 포인트 조회 시 mock 객체 반환 설정
        given(userPointRepository.selectById(userId)).willReturn(expected);

        //when
        UserPoint result = pointService.findPoint(userId); // 검증할 메서드 실행

        //then
        assertThat(result.id()).isEqualTo(userId); // 결과 유저 ID가 예상과 일치하는지 검증
        verify(userPointRepository).selectById(eq(userId)); // 해당 메서드가 호출되었는지 검증
    }

    @Test
    @DisplayName("유저가 포인트를 사용하면 금액만큼 차감하여 객체를 반환한다.")
    void usePoint() {
        //given
        long userId = USER_ID;
        long chargeAmount = 10_000L; // 초기 충전 금액
        long useAmount = 1_000L; // 사용할 금액
        long expectedAmount = 9_000L; // 예상되는 최종 포인트
        UserPoint userPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis()); // 초기 유저 포인트 객체

        // 유저의 현재 포인트 조회 시 mock 객체 반환 설정
        given(userPointRepository.selectById(userId)).willReturn(userPoint);
        // 포인트 충전 후 예상되는 유저 포인트 객체 반환 설정
        given(userPointRepository.insertOrUpdate(userId, expectedAmount))
                .willReturn(new UserPoint(userId, expectedAmount, System.currentTimeMillis()));

        //when
        UserPoint result = pointService.useUserPoint(userPoint.id(), useAmount); // 검증할 메서드 실행

        //then
        assertThat(result.point()).isEqualTo(expectedAmount); // 결과 포인트가 예상 금액과 일치하는지 검증
        // 포인트 충전 기록을 남기는지 검증
        verify(pointHistoryRepository).insert(eq(userId), eq(expectedAmount), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전/사용 내역을 조회한다.")
    void getUserPointHistory() {
        //given
        long userId = USER_ID;
        // 예상 포인트 내역 설정
        List<PointHistory> expectedList = List.of(
                new PointHistory(USER_ID, userId, 1_000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis())
        );
        // 유저의 포인트 내역 조회 시 mock 객체 반환 설정
        given(pointHistoryRepository.selectAllByUserId(userId)).willReturn(expectedList);

        //when
        List<PointHistory> result = pointService.getUserPointHistory(userId); // 검증할 메서드 실행

        //then
        assertThat(result)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("updateMillis")
                .isEqualTo(expectedList); // updateMillis 제외하고 비교
        // 포인트 내역 조회 메서드가 호출되었는지 검증
        verify(pointHistoryRepository).selectAllByUserId(eq(userId));
    }
}