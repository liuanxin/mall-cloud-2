package com.github.common.util;

import com.github.common.date.DateFormatType;
import com.github.common.date.DateUtil;
import org.junit.Test;

import java.util.Date;

public class DateTest {

    @Test
    public void date() {
        Date now = DateUtil.now();
        System.out.println(now);
        System.out.println(DateUtil.formatDateTime(now));
        System.out.println(DateUtil.formatDateTimeMs(now));

        String date = DateUtil.formatDate(now);
        System.out.println(date);
        System.out.println(DateUtil.parse(date));

        date = DateUtil.format(now, DateFormatType.YYYYMMDDHHMMSS);
        System.out.println(DateUtil.parse(date));

        long end = DateUtil.parse("2019-01-03 01:02:02").getTime();
        long start = DateUtil.parse("2019-01-01 01:02:03").getTime();
        System.out.println(DateUtil.toHumanRoughly(end - start));
        System.out.println(DateUtil.toHuman(end - start));

        System.out.println(DateUtil.getDayStart(DateUtil.parse("2019-01-03 01:02:02")));
    }
}
