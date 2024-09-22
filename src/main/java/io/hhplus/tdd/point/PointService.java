package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
  
    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    /**
     * 특정 유저의 포인트를 충전한다.
     * @param id
     * @param amount
     * @return 포인트가 충전된 UserPoint 객체 반환
     */
    public UserPoint chargeUserPoint(long id, long amount) {
        return chargeOrUseUserPoint(id, amount, TransactionType.CHARGE);
    }

    public UserPoint useUserPoint(long id, long amount) {
        return chargeOrUseUserPoint(id, -amount, TransactionType.USE);
    }

    public List<PointHistory> getUserPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    private UserPoint chargeOrUseUserPoint(long id, long amount, TransactionType type) {
        UserPoint entity = userPointTable.selectById(id);
        UserPoint userPoint = userPointTable.insertOrUpdate(id, entity.point() + amount);
        pointHistoryTable.insert(id, userPoint.point(), type, System.currentTimeMillis());
        return userPoint;
    }
}
