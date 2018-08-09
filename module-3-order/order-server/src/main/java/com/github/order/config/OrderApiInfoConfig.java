package com.github.order.config;

import com.github.common.json.JsonCode;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.EnableApiInfo;
import com.github.liuanxin.api.model.DocumentCopyright;
import com.github.liuanxin.api.model.DocumentResponse;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
@EnableApiInfo
@ConditionalOnClass(DocumentCopyright.class)
public class OrderApiInfoConfig {

    @Value("${online:false}")
    private boolean online;

    @Bean
    public DocumentCopyright urlCopyright() {
        return new DocumentCopyright()
                .setTitle(Develop.TITLE + " - 订单")
                .setTeam(Develop.TEAM)
                .setCopyright(Develop.COPYRIGHT)
                .setVersion(Develop.VERSION)
                .setOnline(online)
                .setIgnoreUrlSet(ignoreUrl())
                .setGlobalResponse(globalResponse());
    }

    private Set<String> ignoreUrl() {
        return Sets.newHashSet("/error");
    }

    private List<DocumentResponse> globalResponse() {
        List<DocumentResponse> responseList = Lists.newArrayList();
        for (JsonCode code : JsonCode.values()) {
            responseList.add(new DocumentResponse(code.getFlag(), code.getMsg()));
        }
        return responseList;
    }
}
