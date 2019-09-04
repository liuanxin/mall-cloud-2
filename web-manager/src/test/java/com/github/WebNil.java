package com.github;

import com.github.common.util.Compressor;

public class WebNil {

    public static void main(String[] args) throws Exception {
        // String str = Files.asCharSource(new File("/home/admin/api-info-en.html"), StandardCharsets.UTF_8).read();
        String str = "";
        System.out.println(Compressor.html(str));
    }
}
