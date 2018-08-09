package com.github.common.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 html 压缩成一行<br><br>
 * 1. 将页面中的 &lt;!--[if lt IE 7 ]> &lt;html class="no-js ie6"> &lt;![endif]--> 提取出来<br>
 * 2. 将页面中的 &lt;script> 和「&lt;pre> + &lt;textarea>」标签提取出来<br>
 * 3. 替换注释 &lt;-- xxx --> 和 / * * / 成一个空格<br>
 * 4. 把 &lt;!--[if lt IE 7 ]> &lt;html class="no-js ie6"> &lt;![endif]--> 还原回来<br>
 * 5. 把 换行符 替换成一个空格, 再将多个空格替换成一个<br>
 * 6. 把 &lt;pre> &lt;textarea> 还原回去<br>
 * 7. 把 js 代码还原回去(先把字符串里的「//  / *  * /」替换成指定的占位, 再去掉注释, 再把字符串里面的指定占位还原回去)<br>
 *
 * @author https://github.com/liuanxin/
 */
public final class Compressor {

    private static final String CLAZZ_NAME = Compressor.class.getName();


    private static final String SPACE = " ";
    private static final Pattern LINE_REGEX = compile("\\n|\\r");
    private static final Pattern MULTI_SPACE_REGEX = compile("\\s{2,}");


    /** 页面上的 &lt;!-- ... --&gt; 注释正则 */
    private static final Pattern PAGE_REGEX = compile("(?s)<!--.*?-->");
    /** css 或 js 中的 / * ...  * / 注释正则 */
    private static final Pattern CSS_JS_REGEX = compile("(?s)/\\*.*?\\*/");
    /** js 中的 // 注释正则 */
    private static final Pattern SINGLE_JS_REGEX = compile("//.*");


    /** 页面中的 &lt;!--[if lt IE 7 ]> &lt;html class="ie ie6"> &lt;![endif]--> 代码 */
    private static final Pattern IF_REGEX = compile("(?is)<!--\\s*?\\[(if|else).*?if\\]\\s*?-->");
    /** if 标签的占位符 */
    private static final String IF_PLACE = place("if");
    /** 将 if 标签的占位符还原回去的正则 */
    private static final Pattern IF_PLACE_REGEX = compile(IF_PLACE);


    /** 页面中的 pre 代码 */
    private static final Pattern PRE_REGEX = compile("(?is)<pre.*?>(.*?)</pre.*?>");
    /** pre 标签的占位符 */
    private static final String PRE_PLACE = place("pre");
    /** 将 pre 标签的占位符还原回去的正则 */
    private static final Pattern PRE_PLACE_REGEX = compile(PRE_PLACE);

    /** 页面中的 textarea 代码 */
    private static final Pattern TEXTAREA_REGEX = compile("(?is)<textarea.*?>(.*?)</textarea.*?>");
    /** textarea 标签的占位符 */
    private static final String TEXTAREA_PLACE = place("textarea");
    /** 将 textarea 标签的占位符还原回去的正则 */
    private static final Pattern TEXTAREA_PLACE_REGEX = compile(TEXTAREA_PLACE);


    /** 获取页面中 js 的正则 */
    private static final Pattern SCRIPT_REGEX = compile("(?is)(<script.*?>)(.*?)(</script.*?>)");
    /** js 标签的占位符 */
    private static final String SCRIPT_PLACE = place("script");
    /** 将 js 标签的占位符还原回去的正则 */
    private static final Pattern SCRIPT_PLACE_REGEX = compile(SCRIPT_PLACE);

    /** js 代码中的字符串 // 匹配 */
    private static final Pattern SCRIPT_STR_SINGLE_REGEX = compile("(\".*?)(//)(.*?\")");
    private static final Pattern SCRIPT_STR_SINGLE_APOSTROPHE_REGEX = compile("('.*?)(//)(.*?')");
    /** js 代码中的单行注释字符串占位符 */
    private static final String SCRIPT_STR_SINGLE_PLACE = place("script_str_single");
    /** js 代码中的单行注释字符串标签 */
    private static final Pattern SCRIPT_STR_SINGLE_PLACE_REGEX = compile(SCRIPT_STR_SINGLE_PLACE);

    /** js 代码中的字符串 /* 匹配 */
    private static final Pattern SCRIPT_STR_MULTI_START_REGEX = compile("(\".*?)(/\\*)(.*?\")");
    private static final Pattern SCRIPT_STR_MULTI_START_APOSTROPHE_REGEX = compile("('.*?)(/\\*)(.*?')");
    /** js 代码中的多行注释开始字符串占位符 */
    private static final String SCRIPT_STR_MULTI_START_PLACE = place("script_str_multi_start");
    /** js 代码中的多行注释开始字符串标签 */
    private static final Pattern SCRIPT_STR_MULTI_START_PLACE_REGEX = compile(SCRIPT_STR_MULTI_START_PLACE);

    /** js 代码中的字符串 * / 匹配 */
    private static final Pattern SCRIPT_STR_MULTI_END_REGEX = compile("(\".*?)(\\*/)(.*?\")");
    private static final Pattern SCRIPT_STR_MULTI_END_APOSTROPHE_REGEX = compile("('.*?)(\\*/)(.*?')");
    /** js 代码中的多行注释结束字符串占位符 */
    private static final String SCRIPT_STR_MULTI_END_PLACE = place("script_str_multi_end");
    /** js 代码中的多行注释结束字符串标签 */
    private static final Pattern SCRIPT_STR_MULTI_END_PLACE_REGEX = compile(SCRIPT_STR_MULTI_END_PLACE);


    private static String place(String tag) {
        return String.format("___%s-%s===", CLAZZ_NAME, tag);
    }
    private static Pattern compile(String regex) {
        return Pattern.compile(regex);
    }


    /** 压缩 html 代码成一行 */
    public static String html(String html) {
        if (U.isBlank(html)) {
            return U.EMPTY;
        }

        // 收集 <script> 标签. 这个特殊的标签里面的内容要单独处理
        List<String> scriptList = A.linkedLists();

        // 收集 if 标签: <!--[if lt IE 7 ]> <html class="ie ie6"> <![endif]-->
        List<String> ifList = A.linkedLists();
        // 收集 <pre> <textarea> 标签, 这些特殊标签里面的内容不需要压缩, 压缩将会导致内容显示错误
        List<String> preList = A.linkedLists();
        List<String> textareaList = A.linkedLists();

        // 先收集 pre textarea 再收集 js, 最后收集 if
        html = replace(html, PRE_REGEX, preList, PRE_PLACE);
        html = replace(html, TEXTAREA_REGEX, textareaList, TEXTAREA_PLACE);
        html = replaceJs(html, scriptList);
        html = replace(html, IF_REGEX, ifList, IF_PLACE);

        // 去掉页面中 <!-- xx --> 及 css 中的 /* xx */ 注释
        html = replacePageAnnotation(html);
        // 最后把 if 还原回来
        html = receive(html, IF_PLACE_REGEX, ifList);
        // 去掉空白(换行制表空格等)
        html = replaceBlank(html);
        // 把 js 操作成一行了还原回来(如果 js 中也有 if 就不会处理)
        html = receiveJs(html, scriptList);
        // 把 <textarea> <pre> 还原回来
        html = receive(html, TEXTAREA_PLACE_REGEX, textareaList);
        html = receive(html, PRE_PLACE_REGEX, preList);

        return html.trim();
    }

    private static String replace(String html, Pattern pattern, List<String> ignoreList, String place) {
        Matcher match = pattern.matcher(html);
        while (match.find()) {
            ignoreList.add(match.group());
        }
        return match.replaceAll(place);
    }
    private static String receive(String html, Pattern pattern, List<String> ignoreList) {
        while (html.contains(pattern.pattern())) {
            html = replaceFirst(pattern, ignoreList.remove(0), html);
        }
        return html;
    }

    private static String replaceJs(String html, List<String> ignoreList) {
        Matcher match = SCRIPT_REGEX.matcher(html);
        boolean flag = false;
        while (match.find()) {
            ignoreList.add(match.group(2));
            flag = true;
        }
        return flag ? match.replaceAll("$1" + Matcher.quoteReplacement(SCRIPT_PLACE) + "$3") : html;
    }
    private static String replaceComment(Pattern pattern, String place, String content) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            content = matcher.replaceAll("$1" + Matcher.quoteReplacement(place) + "$3");
        }
        return content;
    }
    private static String receiveJs(String html, List<String> ignoreList) {
        if (U.isNotBlank(html) && A.isNotEmpty(ignoreList)) {
            String place;
            while (html.contains(SCRIPT_PLACE)) {
                place = ignoreList.remove(0);
                if (U.isNotBlank(place)) {
                    // 把 js 字符串中 // 或 /* */ 替换掉
                    place = replaceComment(SCRIPT_STR_SINGLE_REGEX, SCRIPT_STR_SINGLE_PLACE, place);
                    place = replaceComment(SCRIPT_STR_SINGLE_APOSTROPHE_REGEX, SCRIPT_STR_SINGLE_PLACE, place);

                    place = replaceComment(SCRIPT_STR_MULTI_START_REGEX, SCRIPT_STR_MULTI_START_PLACE, place);
                    place = replaceComment(SCRIPT_STR_MULTI_START_APOSTROPHE_REGEX, SCRIPT_STR_MULTI_START_PLACE, place);

                    place = replaceComment(SCRIPT_STR_MULTI_END_REGEX, SCRIPT_STR_MULTI_END_PLACE, place);
                    place = replaceComment(SCRIPT_STR_MULTI_END_APOSTROPHE_REGEX, SCRIPT_STR_MULTI_END_PLACE, place);

                    // 去掉 js 中的 多行 及 单行 注释
                    place = replaceJsAnnotation(place);
                    // 去掉空白(换行制表空格等)
                    place = replaceBlank(place);

                    // 把 js 代码中的字符串还原回去
                    place = replaceAll(SCRIPT_STR_SINGLE_PLACE_REGEX, "//", place);
                    place = replaceAll(SCRIPT_STR_MULTI_START_PLACE_REGEX, "/*", place);
                    place = replaceAll(SCRIPT_STR_MULTI_END_PLACE_REGEX, "*/", place);
                }
                // 把 html 里面的 script 占位还原回去
                html = replaceFirst(SCRIPT_PLACE_REGEX, place, html);
            }
        }
        return html;
    }

    private static String replacePageAnnotation(String content) {
        // 将页面中 /* xx */ 和 <!-- xx --> 替换成 空格
        content = replaceJsOrCssAnnotation(content);
        return replaceAll(PAGE_REGEX, SPACE, content);
    }
    private static String replaceJsOrCssAnnotation(String content) {
        // 将 /* xx */ 替换成 空格
        return replaceAll(CSS_JS_REGEX, SPACE, content);
    }
    private static String replaceJsAnnotation(String content) {
        // 将 js 中的 /* xx * / 和 // xx 替换成空格
        // 先替换多行(/**/), 避免先替换单行(//)时, 把   /* http://abc.com * /   替换成了   /* http:   导致后面出错
        content = replaceJsOrCssAnnotation(content);
        return replaceAll(SINGLE_JS_REGEX, SPACE, content);
    }
    private static String replaceBlank(String content) {
        // 将 换行 替换成一个空格, 再将 多个空白符 替换成一个空格
        content = replaceAll(LINE_REGEX, SPACE, content);
        return replaceAll(MULTI_SPACE_REGEX, SPACE, content);
    }
    private static String replaceAll(Pattern pattern, String place, String content) {
        // content.replaceAll(regex, place) ==> Pattern.compile(regex).matcher(content).replaceAll(place);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            content = matcher.replaceAll(place);
        }
        return content;
    }
    private static String replaceFirst(Pattern pattern, String place, String content) {
        // content.replaceFirst(regex, place) ==> Pattern.compile(regex).matcher(content).replaceFirst(place);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            content = matcher.replaceFirst(Matcher.quoteReplacement(place));
        }
        return content;
    }
}
