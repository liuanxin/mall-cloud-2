package com.github.common.sql;

import com.github.common.date.DateUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.mysql.cj.MysqlConnection;
import com.mysql.cj.Query;
import com.mysql.cj.interceptors.QueryInterceptor;
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
    public <T extends Resultset> T postProcess(Supplier<String> sql, Query interceptedQuery,
                                               T originalResultSet, ServerSession serverSession) {
        if (U.isNotBlank(sql)) {
            if (LogUtil.SQL_LOG.isDebugEnabled()) {
                String formatSql = SqlFormat.format(sql.get());
                Long start = TIME.get();
                if (start != null) {
                    LogUtil.SQL_LOG.debug("time: {}, sql:\n{}", DateUtil.toHuman(System.currentTimeMillis() - start), formatSql);
                } else {
                    LogUtil.SQL_LOG.debug("sql:\n{}", formatSql);
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
