package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final Map<String, Lock> userLocks = new ConcurrentHashMap<>();
  
    public UserPoint findPoint(long id) {
        return UserPoint.findById(id, userPointRepository);
    }

    public UserPoint charge(long id, long amount) {
        Lock lock = userLocks.computeIfAbsent("" + id, k -> new ReentrantLock(true));
        log.info("charge lock: {}", id);
        lock.lock();
        try {
            log.info("charging {}", id);
            UserPoint userPoint = UserPoint.findById(id, userPointRepository);
            UserPoint updateUserPoint = userPoint.charge(amount);

            PointHistory pointHistory = PointHistory.create(id, amount, TransactionType.CHARGE);
            pointHistory.save(pointHistoryRepository);

            userPointRepository.saveOrUpdate(id, updateUserPoint.point());

            return updateUserPoint;
        } finally {
            lock.unlock();
        }
    }

    public UserPoint use(long id, long amount) {
        Lock lock = userLocks.computeIfAbsent("" + id, k -> new ReentrantLock(true));
        lock.lock();
        try {
            UserPoint userPoint = UserPoint.findById(id, userPointRepository);
            UserPoint updateUserPoint = userPoint.use(amount);

            PointHistory pointHistory = PointHistory.create(id, amount, TransactionType.USE);
            pointHistory.save(pointHistoryRepository);

            userPointRepository.saveOrUpdate(id, updateUserPoint.point());

            return updateUserPoint;
        } finally {
            lock.unlock();
        }
    }

    public List<PointHistory> findHistory(long id) {
        UserPoint.findById(id, userPointRepository);
        return pointHistoryRepository.findAllById(id);
    }

}
