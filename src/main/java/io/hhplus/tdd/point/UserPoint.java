package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.UserPointRepository;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    private static final long MAX_POINT = 1_000_000L; // 최대 포인트 잔고 100만

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    // 포인트 충전 및 검증 로직
    public UserPoint charge(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전할 포인트는 0보다 커야 합니다.");
        }
        long totalAmount = this.point + amount;
        if (totalAmount > MAX_POINT) {
            throw new IllegalArgumentException("포인트 최대 잔고는 " + MAX_POINT + "포인트 입니다. 잔액: " + this.point);
        }
        return new UserPoint(this.id, this.point + amount, System.currentTimeMillis());
    }

    // 포인트 사용 및 검증 로직
    public UserPoint use(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용할 포인트는 0보다 커야 합니다.");
        }
        long havePoint = this.point - amount;
        if (havePoint < 0) {
            throw new IllegalArgumentException("포인트 잔액이 부족합니다. 잔액: " + this.point);
        }
        return new UserPoint(this.id, havePoint, System.currentTimeMillis());
    }
}
