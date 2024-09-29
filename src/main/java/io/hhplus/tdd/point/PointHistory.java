package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.PointHistoryRepository;

public record PointHistory(
        long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {
    // 포인트 히스토리 객체 생성
    public static PointHistory create(long userId, long amount, TransactionType type) {
        return new PointHistory(System.currentTimeMillis(), userId, amount, type, System.currentTimeMillis());
    }

    // 포인트 히스토리를 저장
    public void save(PointHistoryRepository pointHistoryRepository) {
        pointHistoryRepository.save(this.userId, this.amount, this.type, this.updateMillis);
    }
}
