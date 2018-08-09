package com.github.common.util;

import com.github.common.date.DateUtil;
import com.google.common.collect.Maps;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class FileUtil {

    /** 保存单个文件到指定的位置, 并将此文件的 url 地址返回 */
    public static String save(MultipartFile file, String directoryPrefix, String urlPrefix) {
        // 保存目录以 / 开头, 结尾不带 /
        directoryPrefix = U.addPrefix(directoryPrefix.trim());
        if (directoryPrefix.endsWith("/")) {
            directoryPrefix = directoryPrefix.substring(0, directoryPrefix.length() - 1);
        }
        // 访问地址前缀以 // 开头, 结尾不带 /
        urlPrefix = urlPrefix.trim();
        if (!urlPrefix.startsWith("http://") && !urlPrefix.startsWith("https://")) {
            urlPrefix = "//" + urlPrefix;
        } else {
            urlPrefix = urlPrefix.replaceFirst("http(s?)://", "//");
        }
        if (urlPrefix.endsWith("/")) {
            urlPrefix = urlPrefix.substring(0, urlPrefix.length() - 1);
        }

        // 保存及访问地址中拼上 /年/月/日/ 按日来保存文件, 前后都要有 /
        String middlePath = "/" + DateUtil.formatUsaDate(DateUtil.now()) + "/";
        // 目录不存在就生成
        File directory = new File(directoryPrefix + middlePath);
        if (!directory.exists()) {
            // noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
        // 重命名
        String newName = U.renameFile(file.getOriginalFilename());

        try {
            file.transferTo(new File(directory, newName));
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("upload file exception", e);
            }
            U.assertException("文件上传时异常");
        }
        return urlPrefix + middlePath + newName;
    }

    /** 保存多个文件到指定的位置, 并将 { 原文件名1: url 地址 } 返回 */
    public static Map<String, String> save(List<MultipartFile> fileList, String directoryPrefix, String urlPrefix) {
        Map<String, String> nameUrlMap = Maps.newHashMap();
        for (MultipartFile file : fileList) {
            String url = save(file, directoryPrefix, urlPrefix);
            nameUrlMap.put(file.getOriginalFilename(), url);
        }
        return nameUrlMap;
    }
}
