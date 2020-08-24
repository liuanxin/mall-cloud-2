package com.github.global.service;

import com.github.common.util.U;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnClass({ RedisTemplate.class, StringRedisTemplate.class })
public class CacheService {

    /** @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration */
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<Object, Object> redisTemplate;

    public CacheService(StringRedisTemplate stringRedisTemplate, RedisTemplate<Object, Object> redisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTemplate = redisTemplate;
    }

    /** 往 redis 中放值 */
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }
    /** 往 redis 放值, 并设定超时时间 */
    public void set(String key, String value, long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, time, unit);
    }
    /** 往 redis 放值, 并设定超时时间 */
    public void set(String key, String value, Date expireTime) {
        if (expireTime != null) {
            Date now = new Date();
            if (expireTime.after(now)) {
                set(key, value, now.getTime() - expireTime.getTime(), TimeUnit.MILLISECONDS);
            }
        }
    }

    public void expire(String key, long time, TimeUnit timeUnit) {
        stringRedisTemplate.expire(key, time, timeUnit);
    }

    public long incr(String key) {
        Long inc = stringRedisTemplate.opsForValue().increment(key, 1L);
        return inc == null ? 0 : inc;
    }

    /**
     * <pre>
     * 用 redis 获取分布式锁, 获取成功则返回 true
     *
     * String key = "xxx", value = uuid(); // value 用 uuid 确保每个线程都不一样
     * if (tryLock(key, value)) {
     *   try {
     *     // 获取到锁之后的业务处理
     *   } finally {
     *     // 解锁时 key 和 value 都需要
     *     unlock(key, value);
     *   }
     * }
     * </pre>
     * @param key 键
     * @param value 值
     */
    public boolean tryLock(String key, String value) {
        return tryLock(key, value, 10, TimeUnit.SECONDS, 1, 1);
    }
    /**
     * <pre>
     * 用 redis 获取分布式锁, 获取成功则返回 true
     *
     * String key = "xxx", value = uuid(); // value 用 uuid 确保每个线程都不一样
     * int time = 3;
     * if (tryLock(key, value, 10, TimeUnit.SECONDS, time, 10)) {
     *   try {
     *     // 获取到锁之后的业务处理
     *   } finally {
     *     // 解锁时 key 和 value 都需要
     *     unlock(key, value);
     *   }
     * } else {
     *   LOG.info("操作了 {} 次依然没有获得锁", time);
     * }
     * </pre>
     *
     * @param key 键
     * @param value 值
     * @param time 超时时间
     * @param unit 时间单位
     * @param retryTime 重试次数
     * @param sleepTime 每次重试时, 休眠毫秒数
     * @return 返回 true 则表示获取到了锁
     */
    public boolean tryLock(String key, String value, long time, TimeUnit unit, int retryTime, long sleepTime) {
        int retry = (retryTime <= 0 || retryTime > 10) ? 1 : retryTime;
        long sleep = (sleepTime <= 0 || sleepTime > 1000) ? 10L : sleepTime;

        String script = "if redis.call('set', KEYS[1], KEYS[2], 'PX', KEYS[3], 'NX') then return 1; else return 0; end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        List<String> keys = Arrays.asList(key, value, String.valueOf(unit.toMillis(time)));
        for (int i = 0; i < retry; i++) {
            Long flag = stringRedisTemplate.execute(redisScript, keys);
            if (flag != null && flag == 1L) {
                return true;
            } else {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleep);
                } catch (InterruptedException ignore) {
                }
            }
        }
        return false;
    }
    /**
     * <pre>
     * 用 redis 获取分布式锁, 获取成功则返回 true
     *
     * String key = "xxx", value = uuid(); // value 用 uuid 确保每个线程都不一样
     * if (tryLock(key, value)) {
     *   try {
     *     // 获取到锁之后的业务处理
     *   } finally {
     *     // 解锁时 key 和 value 都需要
     *     unlock(key, value);
     *   }
     * }
     * </pre>
     * @param key 键
     * @param value 值
     */
    public void unlock(String key, String value) {
        // 释放锁的时候先去缓存中取, 如果值跟之前存进去的一样才进行删除操作, 避免当前线程执行太长, 超时后其他线程又设置了值在处理
        String script = "if redis.call('get', KEYS[1]) == KEYS[2] then return redis.call('del', KEYS[1]); else return 0; end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        List<String> keys = Arrays.asList(key, value);
        stringRedisTemplate.execute(redisScript, keys);
    }

    /** 从 redis 中取值 */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }
    /** 从 redis 中删值 */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /** 向队列写值(从左边压栈) */
    public void push(Object key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }
    /** 向队列读值(从右边出栈) */
    public Object pop(Object key) {
        return redisTemplate.opsForList().rightPop(key);
    }
    /** 向队列读值(从右边出栈, 阻塞指定的时间) */
    public Object pop(Object key, int time, TimeUnit unit) {
        return redisTemplate.opsForList().rightPop(key, time, unit);
    }


    /** 获取指定 set 的长度: scard key */
    public long setSize(String key) {
        Long size = stringRedisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }
    /** 将指定的 set 存进 redis 的 set 并返回成功条数: sadd key v1 v2 v3 ... */
    public long setAdd(String key, String[] set) {
        Long add = stringRedisTemplate.opsForSet().add(key, set);
        return add != null ? add : 0L;
    }
    public Set<String> setGet(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }
    public void setRemove(String key, String value) {
        stringRedisTemplate.opsForSet().remove(key, value);
    }
    /** 从指定的 set 中随机取一个值: spop key */
    public Object setPop(String key) {
        return stringRedisTemplate.opsForSet().pop(key);
    }


    public void hashPut(String key, String hashKey, String hashValue) {
        stringRedisTemplate.opsForHash().put(key, hashKey, hashValue);
    }
    public void hashPutIfAbsent(String key, String hashKey, String hashValue) {
        stringRedisTemplate.opsForHash().putIfAbsent(key, hashKey, hashValue);
    }
    public long hashSize(String key) {
        return stringRedisTemplate.opsForHash().size(key);
    }
    public String hashGet(String key, String hashKey) {
        return U.toStr(stringRedisTemplate.opsForHash().get(key, hashKey));
    }
    public void hashIncr(String key, String hashKey) {
        stringRedisTemplate.opsForHash().increment(key, hashKey, 1);
    }
    public void hashRemove(String key, String hashKey) {
        stringRedisTemplate.opsForHash().delete(key, hashKey);
    }
}
