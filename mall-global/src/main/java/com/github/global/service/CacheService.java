package com.github.global.service;

import com.github.common.date.DateUtil;
import com.github.common.util.U;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.ReflectionUtils;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnClass({ Jedis.class, RedisTemplate.class })
@ConditionalOnBean({ RedisTemplate.class, StringRedisTemplate.class })
public class CacheService {

    @Autowired
    private RedisConnectionFactory connectionFactory;

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
     * 向 redis 中原子存放一个值, 成功则返回 true, 否则返回 false, 想要操作分布式锁, 可以像下面这样操作
     *
     * String key = xxx;
     * String value = uuid();
     * long seconds = yyy;
     * boolean flag = cacheService.setIfNotExists(key, value, seconds);
     * if (flag) {
     *   try {
     *     // 获取到锁之后的业务处理
     *   } finally {
     *     // 释放锁的时候先去缓存中取, 如果值跟之前存进去的一样才进行删除操作
     *     // 避免当前线程执行太长, 超时后其他线程又设置了值在处理
     *     // 如果当前线程 不获取并比较就直接删除, 会将其他线程获取到的锁信息也给删掉
     *     String val = cacheService.get(key);
     *     if (value.equals(val)) {
     *       cacheService.delete(key);
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
    public boolean setIfNotExists(String key, String value, long seconds) {
        Field jedisField = ReflectionUtils.findField(JedisConnection.class, "jedis");
        ReflectionUtils.makeAccessible(jedisField);
        Jedis jedis = (Jedis) ReflectionUtils.getField(jedisField, connectionFactory.getConnection());

        String result = jedis.set(key, value, "NX", "EX", seconds);
        return U.isNotBlank(result) && "OK".equalsIgnoreCase(result);
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
        return stringRedisTemplate.opsForSet().size(key);
    }
    /** 将指定的 set 存进 redis 的 set 并返回成功条数: sadd key v1 v2 v3 ... */
    public long setAdd(String key, String[] set) {
        return stringRedisTemplate.opsForSet().add(key, set);
    }
    /** 从指定的 set 中随机取一个值: spop key */
    public Object setPop(String key) {
        return stringRedisTemplate.opsForSet().pop(key);
    }
}
