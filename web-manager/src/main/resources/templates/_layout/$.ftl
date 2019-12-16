
<#macro style csses=[]>
<#list csses as css>
<link rel="stylesheet" href="${Render.url(css)}"/>
</#list>
</#macro>


<#macro script jses=[]>
<#list jses as js>
<script type="text/javascript" src="${Render.url(js)}"></script>
</#list>
</#macro>


<#macro render>
<#local html><#nested></#local>
<#if online>${Render.compress(html!)}<#else>${html!}</#if>
</#macro>


<#--
在具体的页面上使用时 将当前页面的 css 和 js 定义成两个变量

<#import "/layout/$.ftl" as layout>
<#assign css>
<@layout.style ["http://x/y.css", "http://z/m.css"]/> // 引入外部 css 文件
<style rel="stylesheet">                              // 当前页面的 css 内容
html {
    color: red;
}
...
</style>
</#assign>

<#assign js>
<@layout.script ["http://x/y.js", "http://z/m.js"]/> // 引入外部 js 文件
<script type="text/javascript">                      // 当前页面的 js 内容
var some = "xyz";
...
</script>
</#assign>
<@layout.content title="xxx" key="yyy" desc="zzz" pageCss="${css}" pageJs="${js}" >
当前页面的 html 内容
</@layout.content>

最终的输出: css 在头部, js 在最尾, 且 online 变量为 true 时(生产环境) html 将会输出到一行
-->
<#macro content title="" key="" desc="" pageCss="" pageJs="">
<@render>
<!DOCTYPE html>
<html lang="zh-CN">
<head>

<meta charset="utf-8">
<title><#if title != "">${title!} - </#if>xxx</title>
<meta name="author" content="zzz Team">
<meta name="keywords" content="xxx<#if key != ""> ${key!}</#if>"/>
<meta name="description" content="<#if desc != "">${desc!} </#if>xxx"/>

<#-- <link rel="shortcut icon" href="http://xxx.yyy.com/favicon.ico"/> -->
<#-- <@style ["http://cdn.bootcss.com/twitter-bootstrap/3.4.1/css/bootstrap.min.css"]/> -->
${pageCss!}
</head>
<body>
<main class="container">
<#nested/>
</main>
<footer>
<div class="copyright">&copy; ${.now?string('YYYY')} xxx</div>
</footer>
<!--[if lt IE 9]>
<@script [
"http://cdn.bootcss.com/html5shiv/3.7.3/html5shiv.min.js",
"http://cdn.bootcss.com/respond.js/1.4.2/respond.min.js"
] />
<![endif]-->
<#-- <@script ["http://cdn.bootcss.com/twitter-bootstrap/3.4.1/js/bootstrap.min.js"]/> -->
${pageJs!}
</body>
</html>
</@render>
</#macro>
