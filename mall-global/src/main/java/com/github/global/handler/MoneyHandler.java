package com.github.global.handler;

import com.github.common.Money;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MoneyHandler extends BaseTypeHandler<Money> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    Money parameter, JdbcType jdbcType) throws SQLException {
        // 保存的时候使用分(long)
        ps.setLong(i, parameter.getCent());
    }

    @Override
    public Money getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 从数据库里取出来的时候, 将 long 初始化成 money
        return new Money(rs.getLong(columnName));
    }

    @Override
    public Money getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return new Money(rs.getLong(columnIndex));
    }

    @Override
    public Money getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return new Money(cs.getLong(columnIndex));
    }
}
