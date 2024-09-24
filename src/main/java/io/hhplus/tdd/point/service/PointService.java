package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointRepository;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
  
    public UserPoint findPoint(long id) {
        return userPointRepository.selectById(id);
    }

    public UserPoint chargeUserPoint(long id, long amount) {
        return chargeOrUseUserPoint(id, amount, TransactionType.CHARGE);
    }

    public UserPoint useUserPoint(long id, long amount) {
        return chargeOrUseUserPoint(id, -amount, TransactionType.USE);
    }

    public List<PointHistory> getUserPointHistory(long id) {
        return pointHistoryRepository.selectAllByUserId(id);
    }

    private UserPoint chargeOrUseUserPoint(long id, long amount, TransactionType type) {
        UserPoint entity = userPointRepository.selectById(id);
        UserPoint userPoint = userPointRepository.insertOrUpdate(id, entity.point() + amount);
        pointHistoryRepository.insert(id, userPoint.point(), type, System.currentTimeMillis());
        return userPoint;
    }
}
