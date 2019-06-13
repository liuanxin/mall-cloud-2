//package com.github.common.sql;
//
//import com.github.common.util.LogUtil;
//import com.github.common.util.U;
//import com.mysql.jdbc.Connection;
//import com.mysql.jdbc.ResultSetInternalMethods;
//import com.mysql.jdbc.Statement;
//import com.mysql.jdbc.StatementInterceptor;
//
//import java.sql.SQLException;
//import java.util.Properties;
//
///**
// * mysql 5 的连接参数是: &statementInterceptors=com.github.common.sql.ShowSql5Interceptor
// * mysql 8 的连接参数是: &queryInterceptors=com.github.common.sql.ShowSql8Interceptor
// */
//public class ShowSql5Interceptor implements StatementInterceptor {
//
//    private static final ThreadLocal<Long> TIME = new ThreadLocal<>();
//
//    @Override
//    public void init(Connection connection, Properties properties) throws SQLException {}
//
//    @Override
//    public ResultSetInternalMethods preProcess(String sql, Statement statement,
//                                               Connection connection) throws SQLException {
//        TIME.remove();
//        TIME.set(System.currentTimeMillis());
//        return null;
//    }
//
//    @Override
//    public ResultSetInternalMethods postProcess(String sql, Statement statement,
//                                                ResultSetInternalMethods resultSetInternalMethods,
//                                                Connection connection) throws SQLException {
//        if (U.isBlank(sql) && U.isNotBlank(statement)) {
//            sql = statement.toString();
//            if (U.isNotBlank(sql)) {
//                int i = sql.indexOf(':');
//                if (i > 0 ) {
//                    sql = sql.substring(i + 1).trim();
//                }
//            }
//        }
//        /*
//        if (statement != null) {
//            if (statement instanceof PreparedStatement) {
//                try {
//                    sql = ((PreparedStatement) statement).asSql();
//                } catch (SQLException sqlEx) {
//                    if (LogUtil.SQL_LOG.isDebugEnabled()) {
//                        LogUtil.SQL_LOG.debug("show sql exception", sqlEx);
//                    }
//                }
//            }
//        }
//        */
//        if (U.isNotBlank(sql)) {
//            if (LogUtil.SQL_LOG.isDebugEnabled()) {
//                // druid -> SQLUtils.formatMySql
//                String formatSql = SqlFormat.format(sql);
//
//                Long start = TIME.get();
//                if (start != null) {
//                    LogUtil.SQL_LOG.debug("time: {} ms, sql:\n{}", (System.currentTimeMillis() - start), formatSql);
//                } else {
//                    LogUtil.SQL_LOG.debug("sql:\n{}", formatSql);
//                }
//            }
//        }
//        TIME.remove();
//        return null;
//    }
//
//    @Override
//    public boolean executeTopLevelOnly() { return false; }
//
//    @Override
//    public void destroy() {}
//}
