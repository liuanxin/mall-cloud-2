package com.github.global.service;

import com.github.common.util.A;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
@Configuration
@ConditionalOnClass({ RedissonClient.class })
public class RedissonService {

    // https://github.com/redisson/redisson/wiki/11.-Redis-commands-mapping

    private static final Codec USE_CODEC = StringCodec.INSTANCE;

    /** @see org.redisson.spring.starter.RedissonAutoConfiguration */
    private final RedissonClient redisson;


    /** 从 redis 中删值, 对应命令: DEL key */
    public void delete(String key) {
        redisson.getBucket(key, USE_CODEC).delete();
    }
    /** 从 redis 中删值, value 非 string 时尽量用这个. 对应命令: UNLINK key */
    public void unlink(String key) {
        redisson.getBucket(key, USE_CODEC).unlink();
    }

    /** 往 redis 中放值, 对应命令: SET key value */
    public <T> void set(String key, T value) {
        redisson.getBucket(key, USE_CODEC).set(value);
    }
    /** 往 redis 放值, 并设定在什么时间超时, 对应命令: SET key value px ms */
    public <T> void set(String key, T value, Date expireTime) {
        if (expireTime != null) {
            Date now = new Date();
            if (expireTime.after(now)) {
                set(key, value, now.getTime() - expireTime.getTime(), TimeUnit.MILLISECONDS);
            }
        }
    }
    /** 往 redis 放值, 并设定超时时间, 对应命令: SET key value PX ms */
    public <T> void set(String key, T value, long time, TimeUnit unit) {
        redisson.getBucket(key, USE_CODEC).set(value, time, unit);
    }

    /** 设置超时时间, 对应命令: PEXPIRE key ms */
    public void expire(String key, long time, TimeUnit unit) {
        redisson.getBucket(key, USE_CODEC).expire(Duration.of(time, unit.toChronoUnit()));
    }

    /** 获取键的存活时间, 单位: 毫秒, 对应命令: PTTL key */
    public long getExpireMs(String key) {
        return redisson.getBucket(key, USE_CODEC).remainTimeToLive();
    }

    /** 自增, 对应命令: INCR key */
    public long incr(String key) {
        return redisson.getAtomicLong(key).incrementAndGet();
    }
    /** 自增, 对应命令: INCRBY key increment */
    public long incr(String key, int incr) {
        return redisson.getAtomicLong(key).addAndGet(incr);
    }

    /** 自减, 对应命令: DECR key */
    public long decr(String key) {
        return redisson.getAtomicLong(key).decrementAndGet();
    }
    /** 自减, 对应命令: INCRBY key -increment, redisson 没有用 DECRBY key decrement */
    public long decr(String key, int decr) {
        return redisson.getAtomicLong(key).addAndGet(-decr);
    }

    /** 从 redis 中取值, 对应命令: GET key */
    public <T> T get(String key) {
        return (T) redisson.getBucket(key, USE_CODEC).get();
    }


    /**
     * 用 redis 获取分布式锁后运行并返回(释放不用调用方处理)
     * <pre>
     *
     * String key = "xxx";
     * return lockAndRun(key, () -> {
     *     // 获取到锁之后的处理
     *     return xxx;
     * });
     * </pre>
     * @param key 键
     */
    public <T> T lockAndRun(String key, Supplier<T> supplier) {
        lock(key);
        try {
            return supplier.get();
        } finally {
            unlock(key);
        }
    }

    /**
     * 用 redis 获取分布式锁
     * <pre>
     *
     * String key = "xxx";
     *
     * lock(key);
     * try {
     *   // 获取到锁之后的处理
     * } finally {
     *   unlock(key);
     * }
     * </pre>
     * @param key 键
     */
    public void lock(String key) {
        redisson.getLock(key).lock();
    }

    /**
     * 用 redis 获取分布式锁, 并设置锁的占用时间
     * <pre>
     *
     * String key = "xxx";
     * int lockTime = 10;
     * TimeUnit unit = TimeUnit.SECONDS;
     *
     * lock(key, lockTime, unit);
     * try {
     *   // 获取到锁之后的处理, 此锁会保持 10 秒
     * } finally {
     *   unlock(key);
     * }
     * </pre>
     * @param key 键
     */
    public void lock(String key, int lockTime, TimeUnit unit) {
        redisson.getLock(key).lock(lockTime, unit);
    }


    /**
     * 用 redis 获取分布式锁, 获取成功则运行并返回(释放不用调用方处理)
     * <pre>
     *
     * String key = "xxx";
     * return tryLockAndRun(key, () -> {
     *     // 获取到锁之后的处理
     *     return xxx;
     * });
     * </pre>
     * @param key 键
     */
    public <T> T tryLockAndRun(String key, Supplier<T> supplier) {
        if (tryLock(key)) {
            try {
                return supplier.get();
            } finally {
                unlock(key);
            }
        }
        return null;
    }

    /**
     * 用 redis 获取分布式锁, 获取成功则返回 true
     * <pre>
     *
     * String key = "xxx";
     * if (tryLock(key)) {
     *   try {
     *     // 获取到锁之后的处理
     *   } finally {
     *     unlock(key);
     *   }
     * } else {
     *   log.info("未获取到锁");
     * }
     * </pre>
     * @param key 键
     */
    public boolean tryLock(String key) {
        return redisson.getLock(key).tryLock();
    }

    /**
     * 用 redis 获取分布式锁, 获取成功则返回 true, 获取到的锁将会在设定的时间后失效
     * <pre>
     *
     * String key = "xxx";
     * int waitTime = 5;
     * TimeUnit unit = TimeUnit.SECONDS;
     *
     * try {
     *   if (tryLock(key, waitTime, unit)) {
     *     try {
     *       // 在 5 秒之内获取到了锁
     *     } finally {
     *       unlock(key);
     *     }
     *   } else {
     *     log.info("未获取到锁");
     *   }
     * } catch (InterruptedException e) {
     *   log.info("获取锁时线程被中断了");
     * }
     * </pre>
     * @param key 键
     */
    public boolean tryLock(String key, int waitTime, TimeUnit unit) throws InterruptedException {
        return redisson.getLock(key).tryLock(waitTime, unit);
    }

    /**
     * 用 redis 获取分布式锁, 并指定获取锁的时间, 获取到锁后设置锁的占用时间
     * <pre>
     *
     * String key = "xxx";
     * int waitTime = 5;
     * int lockTime = 10;
     * TimeUnit unit = TimeUnit.SECONDS;
     *
     * try {
     *   if (tryLock(key, getTime, waitTime, unit)) {
     *     try {
     *       // 在 5 秒之内获取到了锁, 此锁会保持 10 秒
     *     } finally {
     *       unlock(key);
     *     }
     *   } else {
     *     log.info("未获取到锁");
     *   }
     * } catch (InterruptedException e) {
     *   log.info("获取锁时线程被中断了");
     * }
     * </pre>
     * @param key 键
     */
    public boolean tryLock(String key, int waitTime, int lockTime, TimeUnit unit) throws InterruptedException {
        return redisson.getLock(key).tryLock(waitTime, lockTime, unit);
    }

    /** 释放 redis 锁 */
    public void unlock(String key) {
        redisson.getLock(key).unlock();
    }


    // list: 左进左出则可以构建栈(右进右出也是), 左进右出则可以构建队列(右进左出也是)

    /** 向 list 写值(从左边入), 对应命令: LPUSH key value */
    public <T> void leftPush(String key, T value) {
        redisson.getDeque(key, USE_CODEC).push(value);
    }
    /** 向 list 写值(从右边入), 对应命令: RPUSH key value */
    public <T> void rightPush(String key, T value) {
        redisson.getDeque(key, USE_CODEC).add(value);
    }
    /** 向 list 取值(从左边出), 对应命令: LPOP key */
    public <T> T leftPop(String key) {
        return (T) redisson.getDeque(key, USE_CODEC).pollFirst();
    }
    /** 向 list 取值(从左边出, 并阻塞指定的时间), 对应命令: BLPOP key timeout */
    public <T> T leftPop(String key, int time, TimeUnit unit) throws InterruptedException {
        return (T) redisson.getBlockingDeque(key, USE_CODEC).pollFirst(time, unit);
    }
    /** 向 list 取值(从右边出), 对应命令: RPOP key */
    public <T> T rightPop(String key) {
        return (T) redisson.getDeque(key, USE_CODEC).pollLast();
    }
    /** 向 list 取值(从右边出, 并阻塞指定的时间), 对应命令: BRPOP key timeout */
    public <T> T rightPop(String key, int time, TimeUnit unit) throws InterruptedException {
        return (T) redisson.getBlockingDeque(key, USE_CODEC).pollLast(time, unit);
    }


    // set 的特点是每个元素唯一, 但是不保证排序

    /** 获取指定 set 的长度, 对应命令: SCARD key */
    public long setSize(String key) {
        return redisson.getSet(key, USE_CODEC).size();
    }
    /** 将指定的 set 存进 redis 并返回成功条数, 对应命令: SADD key v1 v2 v3 ... */
    public <T> boolean setAdd(String key, Collection<T> set) {
        return A.isNotEmpty(set) && redisson.getSet(key, USE_CODEC).addAll(set);
    }
    /** 获取 set, 对应命令: SMEMBERS key */
    public <T> Set<T> setGet(String key) {
        return redisson.getSet(key, USE_CODEC);
    }
    /** 从 set 中移除一个值, 对应命令: SREM key value */
    public <T> void setRemove(String key, T value) {
        redisson.getSet(key, USE_CODEC).remove(value);
    }
    /** 从指定的 set 中随机取出一个值, 对应命令: SPOP key */
    public <T> T setPop(String key) {
        return (T) redisson.getSet(key, USE_CODEC).removeRandom();
    }
    /** 从指定的 set 中随机取出一些值, 对应命令: SPOP key count */
    public <T> Set<T> setPop(String key, int count) {
        return (Set<T>) redisson.getSet(key, USE_CODEC).removeRandom(count);
    }


    // hash 键值对

    /** 写 hash, 对应命令: HMSET key field value [field value ...] */
    public <T> void hashPutAll(String key, Map<String, T> hashMap) {
        redisson.getMap(key, USE_CODEC).putAll(hashMap);
    }
    /** 写 hash, 对应命令: HSET key field value */
    public <T> void hashPut(String key, String hashKey, T hashValue) {
        redisson.getMap(key, USE_CODEC).put(hashKey, hashValue);
    }
    /** 写 hash, 只有 hash 中没有这个 key 才能写成功, 有了就不写, 对应命令: HSETNX key field value */
    public <T> void hashPutIfAbsent(String key, String hashKey, T hashValue) {
        redisson.getMap(key, USE_CODEC).putIfAbsent(hashKey, hashValue);
    }
    /** 获取 hash 的长度, 对应命令: HLEN key */
    public int hashSize(String key) {
        return redisson.getMap(key, USE_CODEC).size();
    }
    /** 获取 hash, 对应命令: HGETALL key */
    public <T> Map<String, T> hashGetAll(String key) {
        RMap<String, T> map = redisson.getMap(key, USE_CODEC);
        return map.readAllMap();
    }
    /** 获取 hash 中指定 key 的值, 对应命令: HGET key field */
    public <T> T hashGet(String key, String hashKey) {
        return (T) redisson.getMap(key, USE_CODEC).get(hashKey);
    }
    /** 自增 hash 中指定的 key 的值, 对应命令: HINCRBY key field 1 */
    public void hashIncr(String key, String hashKey) {
        hashIncr(key, hashKey, 1);
    }
    /** 累加 hash 中指定的 key 的值, 对应命令: HINCRBY key field increment */
    public void hashIncr(String key, String hashKey, int incr) {
        redisson.getMap(key, USE_CODEC).addAndGet(hashKey, incr);
    }
    /** 从 hash 中移除指定的 key, 对应命令: HDEL key field */
    public void hashRemove(String key, String hashKey) {
        redisson.getMap(key, USE_CODEC).fastRemove(hashKey);
    }
}
