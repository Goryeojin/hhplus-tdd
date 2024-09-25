package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.UserPointRepository;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return null;
    }

    // 포인트 조회 로직 캡슐화
    public static UserPoint findById(long id, UserPointRepository userPointRepository) {
        UserPoint userPoint = userPointRepository.findById(id);
        if (userPoint == null) {
            throw new IllegalArgumentException("등록되지 않은 유저입니다.");
        }
        return userPoint;
    }

    // 포인트 충전 및 검증 로직
    public UserPoint charge(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("충전할 포인트는 0보다 커야 합니다.");
        }
        return new UserPoint(this.id, this.point + amount, System.currentTimeMillis());
    }

    // 포인트 사용 및 검증 로직
    public UserPoint use(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("사용할 포인트는 0보다 커야 합니다.");
        }
        long havePoint = this.point - amount;
        if (havePoint < 0) {
            throw new IllegalArgumentException("포인트 잔액이 부족합니다. 잔액: " + this.point);
        }
        return new UserPoint(this.id, havePoint, System.currentTimeMillis());
    }
}
