package com.github.common.util;

import com.github.common.date.DateFormatType;
import com.github.common.date.DateUtil;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class NoTest {

    private static final int count = 1000000;

    @Test
    public void generateOrderNo() throws Exception {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        long start = System.currentTimeMillis();
        System.out.println("start order:" + DateUtil.format(new Date(start), DateFormatType.YYYY_MM_DD_HH_MM_SS_SSS));

        List<Callable<String>> callList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            callList.add(NoUtil::getOrderNo);
        }
        List<Future<String>> futures = threadPool.invokeAll(callList, 1, TimeUnit.MINUTES);
        Set<String> set = Sets.newConcurrentHashSet();
        for (Future<String> future : futures) {
            set.add(future.get());
        }
        threadPool.shutdownNow();
        System.out.printf("all : %s\nreal: %s%n", count, set.size());

        long end = System.currentTimeMillis();
        System.out.println("end order : " + DateUtil.format(new Date(end), DateFormatType.YYYY_MM_DD_HH_MM_SS_SSS));
        System.out.println("耗时: " + ((end - start) / 1000.0));

        int i = 0;
        for (String s : set) {
            if (i >= 200) {
                return;
            }
            i++;
            System.out.println(s);
        }
    }
}
