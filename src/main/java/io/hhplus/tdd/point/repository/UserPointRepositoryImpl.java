package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPointRepositoryImpl implements UserPointRepository {

    private final UserPointTable userPointTable;

    @Override
    public UserPoint findById(Long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public UserPoint saveOrUpdate(long id, long amount) {
        return userPointTable.insertOrUpdate(id, amount);
    }
}
