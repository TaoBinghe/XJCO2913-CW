package com.greengo.service.impl;

import com.greengo.service.DistributedLockService;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedissonDistributedLockService implements DistributedLockService {

    private static final long WAIT_SECONDS = 2L;
    private static final long LEASE_SECONDS = 10L;
    private static final String LOCK_FAILURE_MESSAGE = "System busy, please try again later";

    private final RedissonClient redissonClient;

    public RedissonDistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLocks(List.of(lockKey), action);
    }

    @Override
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T executeWithLocks(Collection<String> lockKeys, Supplier<T> action) {
        List<String> distinctLockKeys = lockKeys == null ? List.of() : lockKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList();
        if (distinctLockKeys.isEmpty()) {
            return action.get();
        }

        RLock lock = createLock(distinctLockKeys);
        boolean locked = false;
        try {
            locked = lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalArgumentException(LOCK_FAILURE_MESSAGE);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(LOCK_FAILURE_MESSAGE);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(LOCK_FAILURE_MESSAGE);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void executeWithLocks(Collection<String> lockKeys, Runnable action) {
        executeWithLocks(lockKeys, () -> {
            action.run();
            return null;
        });
    }

    private RLock createLock(List<String> lockKeys) {
        if (lockKeys.size() == 1) {
            return redissonClient.getLock(lockKeys.get(0));
        }

        RLock[] locks = lockKeys.stream()
                .map(redissonClient::getLock)
                .toArray(RLock[]::new);
        return new RedissonMultiLock(locks);
    }
}
