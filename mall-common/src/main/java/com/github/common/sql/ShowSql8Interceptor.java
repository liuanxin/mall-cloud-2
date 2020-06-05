package com.github.common.sql;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.mysql.cj.MysqlConnection;
import com.mysql.cj.Query;
import com.mysql.cj.ServerPreparedQuery;
import com.mysql.cj.interceptors.QueryInterceptor;
import com.mysql.cj.jdbc.result.ResultSetImpl;
import com.mysql.cj.log.Log;
import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.protocol.ServerSession;

import java.util.Properties;
import java.util.function.Supplier;

/**
 * mysql 5 的连接参数是: &statementInterceptors=com.github.common.sql.ShowSql5Interceptor
 * mysql 8 的连接参数是: &queryInterceptors=com.github.common.sql.ShowSql8Interceptor
 */
public class ShowSql8Interceptor implements QueryInterceptor {

    private static final ThreadLocal<Long> TIME = new ThreadLocal<>();

    @Override
    public QueryInterceptor init(MysqlConnection conn, Properties props, Log log) {
        return this;
    }

    @Override
    public <T extends Resultset> T preProcess(Supplier<String> sql, Query interceptedQuery) {
        TIME.remove();
        TIME.set(System.currentTimeMillis());
        return null;
    }

    @Override
    public <T extends Resultset> T postProcess(Supplier<String> sql, Query query, T rs, ServerSession serverSession) {
        String realSql = U.isNotBlank(sql) ? sql.get() : "";
        if (realSql.contains("?") && query instanceof ServerPreparedQuery && rs instanceof ResultSetImpl) {
            // 如果设置了 useServerPrepStmts 为 true 的话, query 将是 ServerPreparedQuery,
            // 此时通过下面方式获取的 sql 语句中不会有单引号('), 比如应该是 name = '张三' 的将会输出成 name = 张三
            // 且 insert 语句只能输出带 ? 的语句
            String tmp = ((ResultSetImpl) rs).getOwningQuery().toString();

            String colon = ":";
            if (U.isNotBlank(tmp) && tmp.contains(colon)) {
                realSql = tmp.substring(tmp.indexOf(colon) + colon.length()).trim();
            }
        }

        if (U.isNotBlank(realSql)) {
            if (LogUtil.SQL_LOG.isDebugEnabled()) {
                String printSql = SqlFormat.format(realSql);
                Long start = TIME.get();
                if (start != null) {
                    long time = System.currentTimeMillis() - start;
                    LogUtil.SQL_LOG.debug("time: {} ms, sql:\n{}", time, printSql);
                } else {
                    LogUtil.SQL_LOG.debug("sql:\n{}", printSql);
                }
            }
        }
        TIME.remove();
        return null;
    }

    @Override
    public boolean executeTopLevelOnly() { return false; }
    @Override
    public void destroy() {}
}
