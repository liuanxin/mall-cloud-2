package com.github;

public class WebNil {

    public static void main(String[] args) throws Exception {
//        String str = "";
//        System.out.println(Compressor.html(str));

//        String path = "/home/tony/project/github/api-document/src/main/resources/static/";
//        Map<String, String> files = A.maps(
//                "intact-api-info.html", "api-info.html",
//                "intact-api-info-example.html", "api-info-example.html",
//
//                "intact-api-info-en.html", "api-info-en.html",
//                "intact-api-info-en-example.html", "api-info-en-example.html"
//        );
//        for (Map.Entry<String, String> entry : files.entrySet()) {
//            String content = Files.asCharSource(new File(path, entry.getKey()), StandardCharsets.UTF_8).read();
//            String compress = Compressor.html(content) + "\n";
//            Files.write(compress.getBytes(StandardCharsets.UTF_8), new File(path, entry.getValue()));
//            //Files.asByteSink(new File(path, entry.getValue())).write(compress.getBytes(StandardCharsets.UTF_8));
//        }

        System.out.println(String.class.isAssignableFrom(Object.class));
        System.out.println(Object.class.isAssignableFrom(String.class));
    }
}
