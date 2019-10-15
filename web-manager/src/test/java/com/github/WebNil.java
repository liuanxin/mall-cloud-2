package com.github;

import com.github.common.util.A;
import com.github.common.util.Compressor;
import com.google.common.io.Files;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WebNil {

    public static void main(String[] args) throws Exception {
        String path = "/home/admin/project/github/api-document/src/main/resources/static/";
        Map<String, String> files = A.maps(
                "intact-api-info.html", "api-info.html",
                "intact-api-info-example.html", "api-info-example.html",

                "intact-api-info-en.html", "api-info-en.html",
                "intact-api-info-en-example.html", "api-info-en-example.html"
        );
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String content = Files.asCharSource(new File(path, entry.getKey()), StandardCharsets.UTF_8).read();
            String compress = Compressor.html(content) + "\n";
            Files.write(compress.getBytes(StandardCharsets.UTF_8), new File(path, entry.getValue()));
            //Files.asByteSink(new File(path, entry.getValue())).write(compress.getBytes(StandardCharsets.UTF_8));
        }
    }
}
