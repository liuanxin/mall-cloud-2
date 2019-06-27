package com.github.global.service;

import com.github.common.date.DateUtil;
import com.github.common.util.U;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnClass({ Jedis.class, RedisTemplate.class })
@ConditionalOnBean({ RedisTemplate.class, StringRedisTemplate.class })
public class CacheService {

    /** @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    public RedisTemplate<Object, Object> redisTemplate;

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
        Date now = DateUtil.now();
        if (expireTime.after(now)) {
            set(key, value, DateUtil.betweenSecond(now, expireTime), TimeUnit.SECONDS);
        }
    }
    /**
     * <pre>
     * 用 redis 获取分布式锁, 获取成功则返回 true. 想要操作分布式锁, 可以像下面这样操作
     *
     * String key = "xxx", value = uuid(); // value 用 uuid 确保每个线程都不一样
     * boolean lock = tryLock(key, value);
     * // 如果获取失败这里会是 false
     * if (lock) {
     *   try {
     *     // 获取到锁之后的业务处理
     *   } finally {
     *     // 解锁时 key 和 value 都需要
     *     unlock(key, value);
     *   }
     * }
     * </pre>
     *
     * @param key 键
     * @param value 值
     * @return 返回 true 则表示获取到了锁
     */
    public boolean tryLock(String key, String value) {
        /*Boolean flag = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            byte[] byteKey = SafeEncoder.encode(key);
            byte[] byteValue = SafeEncoder.encode(value);
            Expiration expiration = Expiration.from(time, unit);
            RedisStringCommands.SetOption option = RedisStringCommands.SetOption.ifAbsent();

            return connection.set(byteKey, byteValue, expiration, RedisStringCommands.SetOption.ifAbsent());
        });
        return U.isNotBlank(flag) && flag;*/
        return tryLock(key, value, 10, TimeUnit.SECONDS, 1, 10);
    }
    /**
     * @param key 键
     * @param value 值
     * @param time 超时时间
     * @param unit 时间单位
     * @param retryTime 重试次数
     * @param sleepTime 每次重试时, 休眠毫秒数
     * @return 返回 true 则表示获取到了锁
     */
    public boolean tryLock(String key, String value, long time, TimeUnit unit, int retryTime, long sleepTime) {
        if (retryTime <= 0 || retryTime > 10) {
            retryTime = 1;
        }
        if (sleepTime <= 0 || sleepTime > 1000) {
            sleepTime = 10;
        }

        String script = "if redis.call('set', KEYS[1], KEYS[2], 'PX', KEYS[3], 'NX') then return 1; else return 0; end";
        RedisScript<Integer> redisScript = new DefaultRedisScript<>(script, Integer.class);
        List<Object> keys = Lists.newArrayList(key, value, unit.toMillis(time));
        for (int i = 0; i < retryTime; i++) {
            Integer flag = redisTemplate.execute(redisScript, keys);
            if (flag != null && flag == 1) {
                return true;
            } else {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignore) {
                }
            }
        }
        return false;
    }
    /**
     * <pre>
     * 用 redis 解分布式锁. 想要操作分布式锁, 可以像下面这样操作
     *
     * String key = "xxx", value = uuid(); // value 用 uuid 确保每个线程都不一样
     * boolean lock = tryLock(key, value, 10L, TimeUnit.SECONDS);
     * // 如果获取失败这里会是 false
     * if (lock) {
     *   try {
     *     // 获取到锁之后的业务处理
     *   } finally {
     *     // 解锁时 key 和 value 都需要
     *     unlock(key, value);
     *   }
     * }
     * </pre>
     *
     * @param key 键
     * @param value 值
     */
    public void unlock(String key, String value) {
        // 释放锁的时候先去缓存中取, 如果值跟之前存进去的一样才进行删除操作, 避免当前线程执行太长, 超时后其他线程又设置了值在处理
        /*String val = get(key);
        if (value.equals(val)) {
            delete(key);
        }*/

        String script = "if redis.call('get', KEYS[1]) == KEYS[2] then return redis.call('del', KEYS[1]); else return 0; end";
        redisTemplate.execute(new DefaultRedisScript<>(script, Integer.class), Lists.newArrayList(key, value));
    }

    /** 设置超时 */
    public void expire(String key, int time, TimeUnit unit) {
        redisTemplate.expire(key, time, unit);
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
        return U.greater0(size) ? size : 0L;
    }
    /** 将指定的 set 存进 redis 的 set 并返回成功条数: sadd key v1 v2 v3 ... */
    public long setAdd(String key, String[] set) {
        Long add = stringRedisTemplate.opsForSet().add(key, set);
        return U.greater0(add) ? add : 0L;
    }
    /** 从指定的 set 中随机取一个值: spop key */
    public Object setPop(String key) {
        return stringRedisTemplate.opsForSet().pop(key);
    }
}
