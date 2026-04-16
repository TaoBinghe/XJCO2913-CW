package com.greengo.service.impl;

import com.greengo.service.DistributedLockService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalDistributedLockService implements DistributedLockService {

    private static final long WAIT_SECONDS = 2L;
    private static final String LOCK_FAILURE_MESSAGE = "System busy, please try again later";

    private final ConcurrentHashMap<String, ReentrantLock> lockRegistry = new ConcurrentHashMap<>();

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
                .sorted(Comparator.naturalOrder())
                .toList();
        if (distinctLockKeys.isEmpty()) {
            return action.get();
        }

        List<ReentrantLock> acquiredLocks = new ArrayList<>();
        try {
            for (String lockKey : distinctLockKeys) {
                ReentrantLock lock = lockRegistry.computeIfAbsent(lockKey, key -> new ReentrantLock());
                if (!lock.tryLock(WAIT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IllegalArgumentException(LOCK_FAILURE_MESSAGE);
                }
                acquiredLocks.add(lock);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(LOCK_FAILURE_MESSAGE);
        } finally {
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                acquiredLocks.get(i).unlock();
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
}
