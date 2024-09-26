# 항해플러스 [1주차 과제] TDD로 개발하기
> TDD(테스트 주도 개발)를 이용하여 포인트 관리 API를 구현합니다.

## 요구 사항
### API End-Point
✅ PATCH  `/point/{id}/charge` : 포인트를 충전한다.   
✅ PATCH `/point/{id}/use` : 포인트를 사용한다.   
✅ GET `/point/{id}` : 포인트를 조회한다.   
✅ GET `/point/{id}/histories` : 포인트 내역을 조회한다.   

### 기능 요구 사항   
✅ 잔고가 부족할 경우, 포인트 사용은 실패하여야 합니다.  
✅ 동시에 여러 건의 포인트 충전, 이용 요청이 들어올 경우 순차적으로 처리되어야 합니다.

---
## 구현 단계
### `Default`
✅ `/point` 패키지 (디렉토리) 내에 `PointService` 기본 기능 작성   
✅ `/database` 패키지의 구현체는 수정하지 않고, 이를 활용해 기능을 구현   
✅ 각 기능에 대한 단위 테스트 작성   

### `Step 1`
✅ 포인트 충전, 사용에 대한 정책 추가 (잔고 부족, 최대 잔고 등)   
✅ 동시에 여러 요청이 들어오더라도 순서대로 (혹은 한번에 하나의 요청씩만) 제어될 수 있도록 리팩토링   
✅ 동시성 제어에 대한 통합 테스트 작성   

### `Step 2` 동시성 제어
﹖ 동시성 제어를 왜 고려해야 하는가?
> 사용자 포인트 시스템에서 다수의 사용자가 동시에 포인트를 충전하거나 사용할 경우,
> 동시성 문제가 발생하면 사용자의 포인트가 중복으로 처리되거나 잘못된 값으로 저장될 수 있습니다.
> 이러한 동시 처리 환경에서는 데이터 일관성을 유지하고, 동시에 발생할 수 있는 충돌을 방지하는 것이 중요합니다.

여러 스레드가 동시에 공유 자원에 접근하는 경우, 데이터의 일관성을 유지하기 위해 적절한 동시성 제어가 필요합니다.   
Java의 멀티스레드 환경에서 `ReentrantLock`을 사용하여 동시성 제어를 구현하고, `ExecutorService`를 사용해 테스트합니다.

#### `ReentrantLock`, `ConcurrentHashMap`을 이용한 동시성 제어
ReentrantLock은 유연한 동시성 제어 메커니즘을 제공합니다.   
공정성(fairness) 옵션을 통해 오래 대기한 스레드가 먼저 자원을 획득하여 순서를 보장할 수 있습니다.   
전체 시스템에 하나의 글로벌 락을 적용하는 대신, ConcurrentHashMap과 결합하여 사용자별로 고유한 락을 관리합니다. 이를 통해 특정 사용자의 포인트에만 락을 적용하고, 다른 사용자는 동시 작업을 수행할 수 있게 합니다.
``` java
  Lock lock = userLocks.computeIfAbsent(String.valueOf(id), k -> new ReentrantLock(true));
  lock.lock();
  long lockAcquiredTime = System.currentTimeMillis();
  log.info("charge lock acquired: {}", id);
  try {
      UserPoint userPoint = userPointRepository.findById(id);
      UserPoint updateUserPoint = userPoint.charge(amount);

      PointHistory pointHistory = PointHistory.create(id, amount, TransactionType.CHARGE);
      pointHistory.save(pointHistoryRepository);

      userPointRepository.saveOrUpdate(id, updateUserPoint.point());

      return updateUserPoint;
  } finally {
      long tryEndTime = System.currentTimeMillis();
      log.info("charge completed: {}, time taken: {} ms", id, tryEndTime - lockAcquiredTime);
      lock.unlock();
  }
```
포인트 충전과 사용 요청 시 각 사용자에 대한 락을 획득하고 작업을 처리한 후 락을 해제합니다. 이를 통해 동일 사용자가 동시에 여러 포인트 충전 및 사용 요청을 보내더라도 데이터의 무결성을 유지할 수 있습니다.

```
02:46:52.269 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge lock acquired: 1
02:46:52.562 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge completed: 1, time taken: 294 ms
02:46:52.563 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge lock acquired: 1
02:46:52.994 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge completed: 1, time taken: 431 ms
02:46:52.995 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- charge lock acquired: 1
02:46:53.279 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- charge completed: 1, time taken: 284 ms
02:46:53.281 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- use lock acquired: 1
02:46:53.754 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- use completed: 1, time taken: 473 ms
02:46:53.756 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge lock acquired: 2
02:46:53.756 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- use lock acquired: 1
02:46:54.081 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- use completed: 1, time taken: 325 ms
02:46:54.083 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use lock acquired: 1
02:46:54.280 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge completed: 2, time taken: 524 ms
02:46:54.282 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge lock acquired: 2
02:46:54.436 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use completed: 1, time taken: 353 ms
02:46:54.714 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge completed: 2, time taken: 431 ms
02:46:54.716 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- use lock acquired: 2
02:46:55.020 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- use completed: 2, time taken: 304 ms
02:46:55.021 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- charge lock acquired: 2
02:46:55.021 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge lock acquired: 3
02:46:55.114 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- charge completed: 3, time taken: 93 ms
02:46:55.114 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- use lock acquired: 3
02:46:55.436 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- charge completed: 2, time taken: 415 ms
02:46:55.436 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- use lock acquired: 2
02:46:55.604 [pool-1-thread-1] INFO io.hhplus.tdd.point.service.PointService -- use completed: 3, time taken: 490 ms
02:46:55.685 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- use completed: 2, time taken: 249 ms
02:46:55.686 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge lock acquired: 3
02:46:55.686 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use lock acquired: 2
02:46:56.044 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- charge completed: 3, time taken: 358 ms
02:46:56.045 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- use lock acquired: 3
02:46:56.159 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use completed: 2, time taken: 473 ms
02:46:56.477 [pool-1-thread-3] INFO io.hhplus.tdd.point.service.PointService -- use completed: 3, time taken: 432 ms
02:46:56.478 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- charge lock acquired: 3
02:46:57.148 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- charge completed: 3, time taken: 670 ms
02:46:57.149 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use lock acquired: 3
02:46:57.789 [pool-1-thread-2] INFO io.hhplus.tdd.point.service.PointService -- use completed: 3, time taken: 640 ms
```
유저 1, 2, 3의 요청 완료 시간은 비동기 적이나, 다른 스레드에서 동일한 사용자가 접근할 경우 대기 시간을 갖고 가장 오래 대기한 스레드가 다음 순서로 진행됩니다.

외부 Database를 사용하지 않고 인메모리에서 데이터가 저장되며,   
자원의 순차적인 사용과 동시성 제어를 위해 ReentrantLock + ConcurrentHashMap 사용한 방식이 적합하다고 생각하였습니다.   
이를 통해 경합 상태와 데이터 불일치를 방지하고, 사용자별로 적절하게 락을 관리하여   
성능 저하를 최소화하는 방법을 채택하였습니다. 동시성 제어를 위한 테스트 방법 또한 터득할 수 있게 되었습니다.
