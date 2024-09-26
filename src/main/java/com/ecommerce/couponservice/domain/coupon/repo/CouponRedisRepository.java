package com.ecommerce.couponservice.domain.coupon.repo;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CouponRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;
    public static final String WAIT_KEY_PREFIX = "coupon:wait:";
    public static final String ENTER_KEY_PREFIX = "coupon:enter:";
    private final long WAIT_QUEUE_START_INDEX = 0L;
    private final long WAIT_QUEUE_BATCH_SIZE = 10L;

    public void addCouponWaitQueue(Long couponId, Long accountId) {
        String waitKey = getWaitQueueKey(couponId);
        addZSet(waitKey, accountId.toString());
    }

    private void addZSet(String key, String value) {
        redisTemplate
                .opsForZSet()
                .add(key, value, (double) System.currentTimeMillis());
    }

    public long moveFromWaitToEnterQueue(Long couponId) {
        String waitQueueKey = getWaitQueueKey(couponId);
        String enterQueueKey = getEnterQueueKey(couponId);
        AtomicLong movedCount = new AtomicLong(0);

        Set<ZSetOperations.TypedTuple<String>> topWaitingAccountIds = redisTemplate
                .opsForZSet()
                .rangeWithScores(waitQueueKey,
                        WAIT_QUEUE_START_INDEX,
                        WAIT_QUEUE_BATCH_SIZE - 1
                );

        if (topWaitingAccountIds!= null && !topWaitingAccountIds.isEmpty()) {
            List txResults = redisTemplate.execute(new SessionCallback<>() {

                @Override
                public <K, V> List execute(@NonNull RedisOperations<K, V> operations) throws DataAccessException {
                    operations.multi();
                    RedisOperations<String, String> pipelinedOps = (RedisOperations<String, String>) operations;
                    pipelinedOps.opsForZSet().add(enterQueueKey, topWaitingAccountIds);

                    topWaitingAccountIds.forEach(tuple -> pipelinedOps.opsForZSet().remove(waitQueueKey, tuple.getValue()));
                    return operations.exec();
                }
            });

            if (txResults != null && !txResults.isEmpty()) {
                movedCount.set(topWaitingAccountIds.size());
                log.info("Moved {} items from wait queue to enter queue", movedCount.get());
            } else {
                log.warn("Transaction failed or no items were moved");
            }
        }
        return movedCount.get();
    }

    public Set<String> getWaitQueueKeys() {
        String pattern = WAIT_KEY_PREFIX + "*";
        return redisTemplate.keys(pattern);

//        this.redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
//            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).build());
//            while (cursor.hasNext()) {
//                keys.add(new String(cursor.next()));
//            }
//            return keys;
//        });
//
//        return keys;
    }

    public Set<String> getEnterQueueKeys() {
        String pattern = ENTER_KEY_PREFIX + "*";
        return redisTemplate.keys(pattern);
    }

    public Set<ZSetOperations.TypedTuple<String>> getEnterQueueValueAndScore(Long couponId) {
        String enterQueueKey = getEnterQueueKey(couponId);
        return redisTemplate
                .opsForZSet()
                .rangeWithScores(enterQueueKey, 0, -1);
    }

    public void removeEnterQueueValue(Long couponId, Long accountId) {
        String enterQueueKey = getEnterQueueKey(couponId);
        removeZSet(enterQueueKey, accountId.toString());
    }

    private void removeZSet(String key, String value) {
        redisTemplate
                .opsForZSet()
                .remove(key, value);
    }

    private String getWaitQueueKey(Long couponId) {
        return WAIT_KEY_PREFIX + couponId;
    }

    private String getEnterQueueKey(Long couponId) {
        return ENTER_KEY_PREFIX + couponId;
    }
}