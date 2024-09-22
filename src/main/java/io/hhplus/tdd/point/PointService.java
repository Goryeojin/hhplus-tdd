package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
  
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
        return userPointTable.insertOrUpdate(id, amount);
    }
}
