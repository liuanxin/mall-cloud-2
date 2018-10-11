package com.github.common.sql;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ResultSetInternalMethods;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.StatementInterceptor;

import java.sql.SQLException;
import java.util.Properties;

public class ShowSqlInterceptor implements StatementInterceptor {

    private static final ThreadLocal<Long> TIME = new ThreadLocal<>();

    @Override
    public void init(Connection connection, Properties properties) throws SQLException {}

    @Override
    public ResultSetInternalMethods preProcess(String sql, Statement statement,
                                               Connection connection) throws SQLException {
        TIME.remove();
        TIME.set(System.currentTimeMillis());
        return null;
    }

    @Override
    public ResultSetInternalMethods postProcess(String sql, Statement statement,
                                                ResultSetInternalMethods resultSetInternalMethods,
                                                Connection connection) throws SQLException {
        if (statement != null) {
            sql = statement.toString();
            if (U.isNotBlank(sql)) {
                int index = sql.indexOf(':');
                if (index > 0) {
                    sql = sql.substring(index + 1).trim();
                }
            }
        }
        if (U.isNotBlank(sql)) {
            if (LogUtil.SQL_LOG.isDebugEnabled()) {
                Long start = TIME.get();
                // druid -> SQLUtils.formatMySql
                String formatSql = SqlFormat.format(sql);
                if (start != null) {
                    LogUtil.SQL_LOG.debug("time: {} ms, sql:\n{}", (System.currentTimeMillis() - start), formatSql);
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
