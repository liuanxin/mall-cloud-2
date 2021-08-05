package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.json.JsonModule;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ObjectMapper.class)
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class JsonDesensitization {

    /** 是否进行脱敏, 默认进行脱敏 */
    @Value("${json.hasDesensitization:true}")
    private boolean hasDesensitization;

    @Value("${json.desensitizationStrLen:1000}")
    private int desStrLen;
    @Value("${json.desensitizationLeftRightLen:200}")
    private int desLeftRightLen;

    /** 是否进行数据压缩, 默认不压缩 */
    @Value("${json.hasCompress:false}")
    private boolean hasCompress;


    private final ObjectMapper objectMapper;
    private final ObjectMapper desensitizationMapper;

    public JsonDesensitization(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.desensitizationMapper = objectMapper.copy();
        this.desensitizationMapper.registerModule(JsonModule.stringDesensitization(desStrLen, desLeftRightLen));
    }

    public String toJson(Object data) {
        if (U.isNull(data)) {
            return U.EMPTY;
        }

        String json;
        try {
            if (data instanceof String) {
                json = (String) data;
            } else {
                json = (hasDesensitization ? desensitizationMapper : objectMapper).writeValueAsString(data);
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("data desensitization exception", e);
            }
            return U.EMPTY;
        }

        return (hasCompress && U.isNotBlank(json)) ? U.compress(json) : json;
    }
}
