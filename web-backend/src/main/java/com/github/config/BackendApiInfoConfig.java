package com.github.config;

import com.github.common.Const;
import com.github.common.json.JsonCode;
import com.github.common.mvc.AppVersion;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.EnableApiInfo;
import com.github.liuanxin.api.annotation.ParamType;
import com.github.liuanxin.api.model.DocumentCopyright;
import com.github.liuanxin.api.model.DocumentParam;
import com.github.liuanxin.api.model.DocumentResponse;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
@EnableApiInfo
@ConditionalOnClass(DocumentCopyright.class)
public class BackendApiInfoConfig {

    @Value("${online:false}")
    private boolean online;

    @Bean
    public DocumentCopyright urlCopyright() {
        return new DocumentCopyright()
                .setTitle(Develop.TITLE + " - 后端服务")
                .setTeam(Develop.TEAM)
                .setCopyright(Develop.COPYRIGHT)
                .setVersion(Develop.VERSION)
                .setOnline(online)
                .setIgnoreUrlSet(ignoreUrl())
                .setGlobalTokens(tokens())
                .setGlobalResponse(globalResponse());
    }

    private Set<String> ignoreUrl() {
        return Sets.newHashSet("/error");
    }

    private List<DocumentResponse> globalResponse() {
        List<DocumentResponse> responseList = Lists.newArrayList();
        for (JsonCode code : JsonCode.values()) {
            responseList.add(new DocumentResponse(code.getCode(), code.getValue()));
        }
        return responseList;
    }

    private List<DocumentParam> tokens() {
        return Arrays.asList(
                DocumentParam.buildToken(Const.VERSION, "认证数据", "abc-xyz", true),
                DocumentParam.buildToken(Const.VERSION, "接口版本", AppVersion.currentVersion(), false).setParamType(ParamType.Query.name())
        );
    }
}
