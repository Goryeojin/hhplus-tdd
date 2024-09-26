package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepositoryImpl;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.repository.UserPointRepositoryImpl;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentTest { // 동시성 제어 테스트

    PointService pointService;
    UserPointRepository userPointRepository;
    PointHistoryRepository pointHistoryRepository;
    PointHistoryTable pointHistoryTable;
    UserPointTable userPointTable;

    @BeforeEach
    void beforeEach() {
        userPointTable = new UserPointTable();
        userPointRepository = new UserPointRepositoryImpl(userPointTable);
        pointHistoryTable = new PointHistoryTable();
        pointHistoryRepository = new PointHistoryRepositoryImpl(pointHistoryTable);
        pointService = new PointService(userPointRepository, pointHistoryRepository);
    }

    /**
     * 멀티 스레드 환경에서, 다수의 유저가 같은 공유 자원에 접근하는 시도가 있다면
     * 스레드 간 동기화를 통해 동시성을 제어할 필요가 있다.
     * `ReentrantLock`은 스레드 간 동기화를 위해 사용된다. 이때, 공정성을 보장하기 위해 `fair` 옵션을 사용하면
     * 락을 대기 중인 스레드들이 먼저 요청한 순서대로 락을 획득하게 되어, 처리 순서가 보장된다.
     * 이는 데이터의 무결성과 일관성을 유지하는 데 도움을 준다.
     * @throws InterruptedException
     */
    @Test
    @DisplayName("동시에 다른 유저가 충전/사용 요청 시 순차적으로 처리한다.")
    void chargeOrUsePointWithOthersThenSequentially() throws InterruptedException {
        // given
        /*
         * *왜 이 구현체를 사용했는가:**
         * - **ExecutorService**: 다수의 스레드를 관리하기 위한 스레드 풀을 제공한다. `Executors.newFixedThreadPool(32)`은
         *   동시에 여러 스레드를 실행할 수 있게 해주지만, 우리가 정의한 스레드 수(threadCount)에 따라 요청을 병렬로 처리한다.
         * - **CountDownLatch**: 모든 스레드가 완료될 때까지 대기하는 메커니즘을 제공한다. 모든 스레드가 작업을 마칠 때까지
         *   메인 스레드가 기다리게 하여, 스레드가 제대로 실행된 후 검증이 이루어지도록 한다.
         */
        final int threadCount = 3;
        final ExecutorService executorService = Executors.newFixedThreadPool(32);
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(1L, 100);
                    pointService.charge(2L, 10);
                    pointService.charge(3L, 100);
                    pointService.use(1L, 100);
                    pointService.use(2L, 10);
                    pointService.use(3L, 100);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        final UserPoint userPoint = pointService.findPoint(1L);
        final UserPoint userPoint2 = pointService.findPoint(2L);
        final UserPoint userPoint3 = pointService.findPoint(3L);

        /*
         * *테스트 목표:**
         * 서로 다른 유저가 동시에 포인트 충전 및 사용 요청을 보낼 때, 충전과 사용의 순서가 무너지는 상황을 방지하고
         * 각 유저의 최종 포인트가 예상대로 0이 되는지 확인한다.
         */
        // then
        assertThat(userPoint.point()).isEqualTo(0);
        assertThat(userPoint2.point()).isEqualTo(0);
        assertThat(userPoint3.point()).isEqualTo(0);
    }
}
