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
        return userPointRepository.findById(id);
    }

    public UserPoint charge(long id, long amount) {
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
    }

    public UserPoint use(long id, long amount) {
        Lock lock = userLocks.computeIfAbsent(String.valueOf(id), k -> new ReentrantLock(true));
        lock.lock();
        long lockAcquiredTime = System.currentTimeMillis();
        log.info("use lock acquired: {}", id);
        try {
            UserPoint userPoint = userPointRepository.findById(id);
            UserPoint updateUserPoint = userPoint.use(amount);

            PointHistory pointHistory = PointHistory.create(id, amount, TransactionType.USE);
            pointHistory.save(pointHistoryRepository);

            userPointRepository.saveOrUpdate(id, updateUserPoint.point());

            return updateUserPoint;
        } finally {
            long tryEndTime = System.currentTimeMillis();
            log.info("charge completed: {}, time taken: {} ms", id, tryEndTime - lockAcquiredTime);
            lock.unlock();
        }
    }

    public List<PointHistory> findHistory(long id) {
        return pointHistoryRepository.findAllById(id);
    }

}
