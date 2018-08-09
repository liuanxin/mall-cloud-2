package com.github.common.util;

import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>https://github.com/mongodb/mongo-java-driver/blob/master/bson/src/main/org/bson/types/ObjectId.java</p>
 *
 * <p>由以下几个部分组成:</p>
 * <li>生成序列号的行为. 如 D 表示订单, X 表示提现, T 表示退款 等</li>
 * <li>时间规则的 hashcode 值</li>
 * <li>自增值. 这个值基于当前进程是同步的. 基于 concurrent 下的 atomic 类实现, 避免 synchronized 锁</li>
 * <li>机器码 + 进程号合并后的 hashcode 值</li>
 * &nbsp;&nbsp;&nbsp;&nbsp;机器码. 当前机器的 mac 地址. 在多台不同的机器时, 此规则可以区分<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;进程号. 此 jvm 进程号. 在一台机器的多个不同进程时, 此规则可以区分
 *
 * <table border="1">
 *     <caption>ObjectID layout</caption>
 *     <tr>
 *         <td>1</td>
 *         <td>2</td>
 *         <td>3</td>
 *         <td>4</td>
 *     </tr>
 *     <tr>
 *         <td>behavior</td>
 *         <td>time.hashCode.sub</td>
 *         <td>inc</td>
 *         <td>(machine + pid).hashCode.sub</td>
 *     </tr>
 * </table>
 */
public final class NoUtil {

    private static final Logger LOGGER = Logger.getLogger(NoUtil.class.getName());

    /** 机器码 加 进程号 会导致生成的序列号很长, 基于这两个值做一些截取 */
    private static final String MP;
    /** 截取长度 */
    private static final int HORIZONTAL_LEN = 4;
    /** 最大长度 */
    private static final int MAX_LEN = 15;
    static {
        // 机器码 --> 本机 mac 地址的 hashcode 值
        int machineIdentifier = createMachineIdentifier();
        // 进程号 --> 当前运行的 jvm 进程号的 hashcode 值
        int processIdentifier = createProcessIdentifier();

        String mp = Integer.toString(Math.abs((machineIdentifier + "" + processIdentifier).hashCode()));
        MP = (mp.length() > HORIZONTAL_LEN) ? mp.substring(mp.length() - HORIZONTAL_LEN, mp.length()) : mp;
    }

    private static int createMachineIdentifier() {
        // build a 2-byte machine piece based on NICs info
        int machinePiece;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                sb.append(ni.toString());
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    ByteBuffer bb = ByteBuffer.wrap(mac);
                    try {
                        sb.append(bb.getChar());
                        sb.append(bb.getChar());
                        sb.append(bb.getChar());
                    } catch (BufferUnderflowException shortHardwareAddressException) { //NOPMD
                        // mac with less than 6 bytes. continue
                    }
                }
            }
            machinePiece = sb.toString().hashCode();
        } catch (Throwable t) {
            // exception sometimes happens with IBM JVM, use random
            machinePiece = new SecureRandom().nextInt();
            LOGGER.log(Level.WARNING, "Failed to get machine identifier from network interface, using random number instead", t);
        }
        return machinePiece;
    }

    // Creates the process identifier. This does not have to be unique per class loader because
    // NEXT_COUNTER will provide the uniqueness.
    private static int createProcessIdentifier() {
        int processId;
        try {
            String processName = ManagementFactory.getRuntimeMXBean().getName();
            if (processName.contains("@")) {
                processId = Integer.parseInt(processName.substring(0, processName.indexOf('@')));
            } else {
                processId = processName.hashCode();
            }
        } catch (Throwable t) {
            processId = new SecureRandom().nextInt();
            LOGGER.log(Level.WARNING, "Failed to get process identifier from JMX, using random number instead", t);
        }
        return processId;
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HHyyssMMmmdd");
    private static String unitCode() {
        String unit = Integer.toString(Math.abs(DATE_FORMAT.format(new Date()).hashCode()));
        return unit.substring(unit.length() - HORIZONTAL_LEN, unit.length());
    }

    /** 生成序列号的类型 */
    private enum Category {
        /** 订单: 标识, 初始值, 步长, 最大值(只要保证之内, 从初始化值加步长不会超时最大值就不会有重复) */
        Order("D", 16, 3, 10000000);

        String behavior;
        int init, step, max;
        AtomicLong counter;
        Lock lock;
        Category(String behavior, int init, int step, int max) {
            this.behavior = behavior;
            this.init = init;
            this.step = step;
            this.max = max - step;

            counter = new AtomicLong(init);
            lock = new ReentrantLock();
        }
        public String no() {
            long increment = counter.addAndGet(step);
            if (increment >= max) {
                lock.lock();
                try {
                    if (increment >= max) {
                        increment = counter.getAndSet(init);
                    }
                } finally {
                    lock.unlock();
                }
            }
            String no = unitCode() + increment + MP;
            if (no.length() < MAX_LEN) {
                StringBuilder sbd = new StringBuilder();
                for (int i = 0; i < MAX_LEN - no.length(); i++) {
                    sbd.append("0");
                }
                no = unitCode() + sbd.toString() + increment + MP;
            }
            return behavior + no;
        }
    }

    /** 生成订单号 */
    public static String getOrderNo() {
        return Category.Order.no();
    }
}
