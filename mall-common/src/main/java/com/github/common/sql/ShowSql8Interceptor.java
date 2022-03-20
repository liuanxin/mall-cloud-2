package com.github.common.sql;

import com.github.common.date.DateUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mysql.cj.MysqlConnection;
import com.mysql.cj.Query;
import com.mysql.cj.Session;
import com.mysql.cj.conf.HostInfo;
import com.mysql.cj.interceptors.QueryInterceptor;
import com.mysql.cj.log.Log;
import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.protocol.ServerSession;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * mysql 5 的连接参数是: &statementInterceptors=com.github.common.sql.ShowSql5Interceptor
 * mysql 8 的连接参数是: &queryInterceptors=com.github.common.sql.ShowSql8Interceptor
 */
public class ShowSql8Interceptor implements QueryInterceptor {

    private static final String TIME_SPLIT = "~";
    private static final AtomicLong COUNTER = new AtomicLong(0L);
    /** 每条 sql 执行前记录时间戳, 如果使用 ThreadLocal 会有 pre 了但运行时异常不去 post 的情况 */
    private static final Cache<Thread, String> TIME_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES).build();
    private static final Pattern BLANK_REGEX = Pattern.compile("\\s{1,}");

    @Override
    public QueryInterceptor init(MysqlConnection conn, Properties props, Log log) {
        return this;
    }

    @Override
    public <T extends Resultset> T preProcess(Supplier<String> sql, Query query) {
        if (LogUtil.SQL_LOG.isDebugEnabled()) {
            String realSql = getRealSql(sql);
            if (U.isNotNull(realSql)) {
                Thread currentThread = Thread.currentThread();
                long current = System.currentTimeMillis();
                long counter = COUNTER.addAndGet(1);

                TIME_CACHE.put(currentThread, counter + TIME_SPLIT + current);
                String dataSource = "";
                if (U.isNotNull(query)) {
                    Session session = query.getSession();
                    if (U.isNotNull(session)) {
                        HostInfo hostInfo = session.getHostInfo();
                        if (U.isNotNull(hostInfo)) {
                            dataSource = ", 数据源: " + hostInfo.getHost() + ":" + hostInfo.getPort() + "/" + hostInfo.getDatabase();
                        }
                    }
                }
                LogUtil.SQL_LOG.debug("计数: {}{}, sql: {}", counter, dataSource, realSql);
            }
        }
        return null;
    }

    private String getRealSql(Supplier<String> sql) {
        if (U.isNull(sql)) {
            return null;
        }

        // String realSql = SQLUtils.formatMySql(sql.replaceFirst("^\\s*?\n", ""));
        // String realSql = SqlFormat.format(sql.get().replaceFirst("^\\s*?\n", ""));
        String realSql = BLANK_REGEX.matcher(sql.get().replaceFirst("^\\s*?\n", "")).replaceAll(" ");
        int len = realSql.length(), max = 2000, leftRight = 400;
        return len > max ? (realSql.substring(0, leftRight) + " ... " + realSql.substring(len - leftRight, len)) : realSql;
    }

    @Override
    public <T extends Resultset> T postProcess(Supplier<String> sql, Query query, T rs, ServerSession serverSession) {
        if (LogUtil.SQL_LOG.isDebugEnabled()) {
            String realSql = getRealSql(sql);
            if (U.isNotNull(realSql)) {
                Thread currentThread = Thread.currentThread();
                String counterAndTime = TIME_CACHE.getIfPresent(currentThread);
                if (U.isNotNull(counterAndTime)) {
                    try {
                        String[] split = counterAndTime.split(TIME_SPLIT);
                        if (split.length == 2) {
                            long counter = U.toLong(split[0]);
                            long start = U.toLong(split[1]);

                            StringBuilder sbd = new StringBuilder();
                            if (U.greater0(counter)) {
                                sbd.append("计数: ").append(counter);
                            }
                            if (U.greater0(start)) {
                                sbd.append(", 用时: ").append(DateUtil.toHuman(System.currentTimeMillis() - start));
                            }
                            if (U.isNotNull(rs) && rs.hasRows()) {
                                sbd.append(", 返回行数: ").append(rs.getRows().size());
                            }
                            LogUtil.SQL_LOG.debug(sbd.toString());
                        }
                    } finally {
                        TIME_CACHE.invalidate(currentThread);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean executeTopLevelOnly() { return false; }
    @Override
    public void destroy() {
        TIME_CACHE.invalidateAll();
    }
}
