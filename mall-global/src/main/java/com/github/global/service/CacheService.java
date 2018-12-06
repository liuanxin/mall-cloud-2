package com.github.global.service;

import com.github.common.date.DateUtil;
import com.github.common.util.U;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import redis.clients.jedis.Jedis;
import redis.clients.util.SafeEncoder;

import java.util.Date;
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
    public void set(String key, String value, long timeOut, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, value, timeOut, timeUnit);
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
     * String successKey = "success";
     * String successFlag = "1";
     *
     * String success = get(successKey);
     * if (!successFlag.equals(success) {
     *   String key = xxx;
     *   String value = uuid();
     *   long seconds = 10L;
     *   boolean lock = tryLock(key, value, seconds);
     *   if (lock) {
     *     try {
     *       String success = get(successKey);
     *       if (!successFlag.equals(success) {
     *         // 获取到锁之后的业务处理
     *         set(successKey, 1, 10, Time.minutes);
     *       }
     *     } finally {
     *       // 释放锁的时候先去缓存中取, 如果值跟之前存进去的一样才进行删除操作
     *       // 避免当前线程执行太长, 超时后其他线程又设置了值在处理
     *       // 如果当前线程 不获取并比较就直接删除, 会将其他线程获取到的锁信息也给删掉
     *       unlock(key, value);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param key 键
     * @param value 值
     * @param seconds 超时时间, 单位: 秒
     * @return 返回 true 则表示获取到了锁
     */
    public boolean tryLock(String key, String value, long seconds) {
         Boolean flag = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
             byte[] byteKey = SafeEncoder.encode(key);
             byte[] byteValue = SafeEncoder.encode(value);
             Expiration expiration = Expiration.seconds(seconds);
             RedisStringCommands.SetOption option = RedisStringCommands.SetOption.ifAbsent();
             return connection.set(byteKey, byteValue, expiration, option);
         });
         return U.isNotBlank(flag) && flag;
    }
    /**
     * <pre>
     * 用 redis 解分布式锁. 想要操作分布式锁, 可以像下面这样操作
     *
     * String successKey = "success";
     * String successFlag = "1";
     *
     * String success = get(successKey);
     * if (!successFlag.equals(success) {
     *   String key = xxx;
     *   String value = uuid();
     *   long seconds = 10L;
     *   boolean lock = tryLock(key, value, seconds);
     *   if (lock) {
     *     try {
     *       String success = get(successKey);
     *       if (!successFlag.equals(success) {
     *         // 获取到锁之后的业务处理
     *         set(successKey, 1, 10, Time.minutes);
     *       }
     *     } finally {
     *       // 释放锁的时候先去缓存中取, 如果值跟之前存进去的一样才进行删除操作
     *       // 避免当前线程执行太长, 超时后其他线程又设置了值在处理
     *       // 如果当前线程 不获取并比较就直接删除, 会将其他线程获取到的锁信息也给删掉
     *       unlock(key, value);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param key 键
     * @param value 值
     */
    public void unlock(String key, String value) {
        String val = get(key);
        if (value.equals(val)) {
            delete(key);
        }
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
