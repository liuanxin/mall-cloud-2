package com.github.common.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/** 生成验证码 */
public final class SecurityCodeUtil {

    private static final Random RANDOM = new Random();

    /** 验证码库(英文) */
    private static final String WORD = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /** 验证码库(数字) */
    private static final String NUMBER = "0123456789";
    /** 验证码库(数字 + 英文, 不包括小写 l、大写 I、小写 o 和 大写 O, 避免跟数字 1 和 0 相似) */
    private static final String WORD_NUMBER = "0123456789abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    /** 字体 */
    private static final String[] fonts = new String[] {
            "consola",
            "monospace",
            "monaco",
            "Verdana",
            "Helvetica",
            "arial",
            "serif",
            "sans-serif",
            "Times",
            "fixedsys"
    };

    /**
     * <pre>
     * 生成验证码图像对象
     *
     * SecurityCodeUtil.Code code = generateCode(count, style, width, height);
     *
     * // 往 session 里面丢值
     * session.setAttribute("xxx", code.getContent());
     *
     * // 向页面渲染图像
     * response.setDateHeader("Expires", 0);
     * response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
     * response.addHeader("Cache-Control", "post-check=0, pre-check=0");
     * response.setHeader("Pragma", "no-cache");
     * response.setContentType("image/jpeg");
     * javax.imageio.ImageIO.write(code.getImage(), "jpeg", response.getOutputStream());
     * </pre>
     *
     * @param count  验证码数字个数, 最少 4 个. 传空值或传小于 4 的值会使用最少值
     * @param style  图片上的文字: 英文, 数字(num), 英文加数字(n)还是中文(cn), 传空值或传的值不是 num n cn 则会默认是英文
     * @param width  生成的图片宽度, 最小 100. 传空值或传小于 100 的值会使用最小值
     * @param height 生成的图片高度, 最小 30. 传空值或传小于 30 的值会使用最小值
     * @param grb 生成的图片的颜色, 不传则默认是 57,66,108
     * @return 图像
     */
    public static Code generateCode(String count, String style, String width, String height, String grb) {
        int loop = toInt(count);
        int maxCount = 4;
        if (loop < maxCount) {
            loop = maxCount;
        }

        String str;
        if ("w".equalsIgnoreCase(style)) {
            str = WORD;
        } else if ("n".equalsIgnoreCase(style)) {
            str = NUMBER;
        } else {
            str = WORD_NUMBER;
        }

        int widthCount = toInt(width);
        int minWidth = 100;
        if (widthCount < minWidth) {
            widthCount = minWidth;
        }

        int heightCount = toInt(height);
        int minHeight = 30;
        if (heightCount < minHeight) {
            heightCount = minHeight;
        }

        int r = -1, g = -1, b = -1;
        if (U.isNotBlank(grb)) {
            String[] s = grb.split(",");
            if (s.length == 3) {
                r = U.toInt(s[0]);
                g = U.toInt(s[1]);
                b = U.toInt(s[2]);
            }
        }
        if (r < 0 || r > 255) { r = 57; }
        if (g < 0 || g > 255) { g = 66; }
        if (b < 0 || b > 255) { b = 108; }

        // ========== 上面处理参数的默认值 ==========

        BufferedImage image = new BufferedImage(widthCount, heightCount, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.createGraphics();
        // 图像背景填充为灰色
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0, 0, widthCount, heightCount);

        // 画一些干扰线
        int interferenceCount = loop * 10;
        int maxRandom = 256;
        for (int i = 0; i < interferenceCount; i++) {
            graphics.setColor(new Color(RANDOM.nextInt(maxRandom), RANDOM.nextInt(maxRandom), RANDOM.nextInt(maxRandom)));
            graphics.drawLine(RANDOM.nextInt(widthCount), RANDOM.nextInt(heightCount),
                    RANDOM.nextInt(widthCount), RANDOM.nextInt(heightCount));
        }
        graphics.setColor(new Color(r, g, b));

        int x = (widthCount - 8) / (loop + 1);
        int y = heightCount - 5;
        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < loop; i++) {
            String value = String.valueOf(str.charAt(RANDOM.nextInt(str.length())));
            // 字体大小
            graphics.setFont(new Font(fonts[RANDOM.nextInt(fonts.length)], Font.BOLD, heightCount - RANDOM.nextInt(8)));
            graphics.drawString(value, (i + 1) * x, y);
            sbd.append(value);
        }
        return new Code(sbd.toString(), image);
    }

    private static int toInt(String str) {
        if (str == null) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Code {
        private String content;
        private BufferedImage image;
    }
}
