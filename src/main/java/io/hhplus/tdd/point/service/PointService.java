package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.repository.UserPointRepository;
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
        return UserPoint.findById(id, userPointRepository);
    }

    public UserPoint charge(long id, long amount) {
        UserPoint userPoint = UserPoint.findById(id, userPointRepository);
        UserPoint updateUserPoint = userPoint.charge(amount);
        PointHistory pointHistory = PointHistory.create(id, amount, TransactionType.CHARGE);
        pointHistory.save(pointHistoryRepository);
        return updateUserPoint;
    }

    public UserPoint use(long id, long amount) {
        UserPoint userPoint = UserPoint.findById(id, userPointRepository);
        UserPoint updateUserPoint = userPoint.use(amount);
        PointHistory pointHistory = PointHistory.create(id, amount, TransactionType.CHARGE);
        pointHistory.save(pointHistoryRepository);
        return userPoint;
    }

    public List<PointHistory> findHistory(long id) {
        return pointHistoryRepository.findAllById(id);
    }

}
