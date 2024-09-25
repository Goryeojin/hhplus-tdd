package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserPointTest {

    @Mock
    PointService pointService;
    @Mock
    UserPointRepository userPointRepository;

    static final Long USER_ID = 1L;

    @Test
    @DisplayName("특정 유저의 포인트 정보를 리턴한다.")
    void returnUsePointInfoWhenUserExists() {
        //given
        UserPoint expected = new UserPoint(USER_ID, 0L, 0L); // 예상되는 유저 포인트 객체
        given(userPointRepository.findById(USER_ID)).willReturn(expected); // 유저의 현재 포인트 조회 시 mock 객체 반환 설정

        //when
        UserPoint result = UserPoint.findById(USER_ID, userPointRepository);

        //then
        assertThat(result.id()).isEqualTo(expected.id());
        assertThat(result.point()).isEqualTo(expected.point());
    }

    @Test
    @DisplayName("유저가 저장되어 있지 않다면 조회에 실패한다.")
    void failIfUserNotExists() {
        assertThatThrownBy(() -> {
            UserPoint.findById(null, userPointRepository);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("등록되지 않은 유저");
    }

    // 충전 포인트 0 이하 실패
    // 충전 최대 잔고 넘으면 실패
    // 충전 성공
    // 사용 포인트 0 이하 실패
    // 잔고 보다 사용 포인트 넘으면 실패
    // 사용 성공


}
