package com.github.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.ChineseConvert;
import com.github.common.util.NumberUtil;
import com.github.common.util.U;

import java.io.Serializable;

/**
 * 运算全部基于到分的 long 来做, 它的性能比 BigDecimal 要好很多
 * 链式运算时, 如 Money count = xxx.plus(y).lessSelf(z).multiplySelf(n).divideSelf(m) 这种
 * 第一次不要用 self, 后面的都加上 self, 这样只会实例化一次.
 * 如果都不带 self 则每一次运算都将实例化一个 Money 对象
 * 如果都带 self 将会改变第一个调用的值, 如上面的示例如果每次调用都带 self 将会改变 xxx 的值
 */
public class Money implements Serializable {
    private static final long serialVersionUID = 0L;

    /** 元分换算的比例. 左移右移的位数 */
    private static final int SCALE = 2;

    /** 实际存储进数据库的值 */
    private Long cent;

    public Money() {}
    /** 从数据库过来的数据, 使用此构造 */
    public Money(Long cent) {
        this.cent = cent;
    }
    /** 从前台过来的数据转换成金额对象时使用此构造 */
    @JsonCreator
    public Money(String yuan) {
        cent = NumberUtil.yuan2Cent(yuan, SCALE);
    }

    /** 在前台或者在页面上显示 */
    @JsonValue
    @Override
    public String toString() {
        return NumberUtil.cent2Yuan(cent, SCALE);
    }

    /** 输出 1,234,567,890.50 的形式 */
    public String toShow() {
        return NumberUtil.cent2ShowYuan(cent);
    }

    public Long getCent() {
        return cent;
    }
    public void setCent(Long cent) {
        this.cent = cent;
    }


    public Money plus(Money money) {
        return new Money(cent + money.cent);
    }
    public Money plusSelf(Money money) {
        cent += money.cent;
        return this;
    }

    public Money less(Money money) {
        return new Money(cent - money.cent);
    }
    public Money lessSelf(Money money) {
        cent -= money.cent;
        return this;
    }

    public Money multiply(int num) {
        return new Money(cent * num);
    }
    public Money multiplySelf(int num) {
        cent *= num;
        return this;
    }

    public Money divide(int num) {
        return new Money(cent / num);
    }
    public Money divideSelf(int num) {
        cent /= num;
        return this;
    }


    /** 检查金额是否是负数 */
    public void checkEmptyAndNegative() {
        if (cent == null || cent < 0) {
            U.assertException("金额不能是负数");
        }
    }

    /** 输出大写中文 */
    public String toChinese() {
        return ChineseConvert.upperCase(toString());
    }
}
