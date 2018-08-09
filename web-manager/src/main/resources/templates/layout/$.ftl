
<#macro head title="" key="" desc="">
<meta charset="utf-8">
<meta name="Author" content="zzz Team">

<title><#if title != "">${title!} - </#if>xxx</title>
<meta name="keywords" content="xxx<#if key != ""> ${key!}</#if>"/>
<meta name="description" content="<#if desc != "">${desc!} </#if>xxx"/>

<#-- <link rel="shortcut icon" href="${Render.url(domain.getStill(), "favicon.ico")}"/> -->
<#--<@style ["common/global.css"]/>-->
</#macro>

<#macro foot>
<#-- 全局的 js 写在下面 -->
<#--<@script ["common/global.js"]/>-->
</#macro>


<#-- <@style ["x/y.css", "z/m.css"] /> -->
<#macro style csses=[]>
<#list csses as css>
<link rel="stylesheet" href="${Render.url(domain.getStill(), css)}"/>
</#list>
</#macro>

<#-- <@script ["x/y.js", "z/m.js"] /> -->
<#macro script jses=[]>
<#list jses as js>
<script type="text/javascript" src="${Render.url(domain.getStill(), js)}"></script>
</#list>
</#macro>


<#macro render>
<#local html><#nested></#local>
<#if online>${Render.compress(html!)}<#else>${html!}</#if>
</#macro>


<#--
在具体的页面上使用: 将当前页面的 css 和 js 定义成两个变量(如果有的话), 而后使用模板将其传入

<#import "/layout/$.ftl" as layout>
<#assign css>
<@layout.style ["x/y.css", "z/m.css"]/>

<style>
div > btn {
   color: red;
}
</style>
</#assign>
<#assign js>
<@layout.script ["x/y.js", "z/m.js"]/>

<script type="text/javascript">
var some = "xyz";
</script>
</#assign>
<@layout.content title="xxx" key="yyy" desc="zzz" pageCss="${css}" pageJs="${js}" >
当前页面的 html 内容
</@layout.content>
-->
<#macro content title="" key="" desc="" pageCss="" pageJs="">
<@render>
<!DOCTYPE html>
<html>
<head>
<@head title key desc/>
${pageCss!}
</head>
<body>
<#nested/>
<@foot/>
${pageJs!}
</body>
</html>
</@render>
</#macro>
