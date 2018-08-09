package com.github.global.handler;

import com.github.common.enums.Gender;
import com.github.common.util.U;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 当前 handle 将会被装载进 mybatis 的运行上下文中去.
 *
 * @see org.apache.ibatis.type.TypeHandlerRegistry
 * @see org.apache.ibatis.type.EnumTypeHandler
 * @see org.apache.ibatis.type.EnumOrdinalTypeHandler
 */
public class GenderHandler extends BaseTypeHandler<Gender> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Gender parameter,
                                    JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public Gender getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return U.toEnum(Gender.class, rs.getObject(columnName));
    }

    @Override
    public Gender getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return U.toEnum(Gender.class, rs.getObject(columnIndex));
    }

    @Override
    public Gender getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return U.toEnum(Gender.class, cs.getObject(columnIndex));
    }
}
