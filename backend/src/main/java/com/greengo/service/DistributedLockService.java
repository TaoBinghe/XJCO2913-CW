package com.greengo.service;

import java.util.Collection;
import java.util.function.Supplier;

public interface DistributedLockService {

    <T> T executeWithLock(String lockKey, Supplier<T> action);

    void executeWithLock(String lockKey, Runnable action);

    <T> T executeWithLocks(Collection<String> lockKeys, Supplier<T> action);

    void executeWithLocks(Collection<String> lockKeys, Runnable action);
}
