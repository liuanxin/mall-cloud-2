//package com.github.config;
//
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.servlet.mvc.method.annotation.AbstractJsonpResponseBodyAdvice;
//
//@ControllerAdvice
//public class BackendJsonpHandler extends AbstractJsonpResponseBodyAdvice {
//
//    /**
//     * request 请求中用到的 jsonp 的地方使用的参数名. 如
//     * <table border="1">
//     *     <tr>
//     *         <td>请求</td>
//     *         <td>返回</td>
//     *     </tr>
//     *     <tr>
//     *         <td>/xxx</td>
//     *         <td>{"id":1,"name":"张三"}</td>
//     *     </tr>
//     *     <tr>
//     *         <td>/xxx?jsonp=user</td>
//     *         <td>user({"id":1,"name":"张三"});</td>
//     *     </tr>
//     *     <tr>
//     *         <td>/xxx?callback=user</td>
//     *         <td>user({"id":1,"name":"张三"});</td>
//     *     </tr>
//     *     <tr>
//     *         <td>/xxx?callback=list</td>
//     *         <td>list({"id":1,"name":"张三"});</td>
//     *     </tr>
//     * </table>
//     */
//    public BackendJsonpHandler() {
//        super("jsonp", "callback");
//    }
//}
