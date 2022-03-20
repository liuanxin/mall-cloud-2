package com.github.global.util;

import com.github.common.exception.ParamException;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Map;
import java.util.Set;

/** 不放进 ValidationUtil 里面是因为这个类依赖 validation 包 */
public class ValidationExtendUtil {

    /**
     * 字段标 @NotNull @Email 等注解, 嵌套字段上要标 @Valid 注解
     *
     * 1. 自动验证: 在方法参数上标 @Validated 注解
     * 2. 手动验证: 注入 Validator, 验证时调用此方法
     */
    public static <T> void handleValidate(Validator validator, T obj, Class<?>... groups) {
        if (U.isNull(obj)) {
            throw new ParamException("参数不能为空");
        }
        Set<ConstraintViolation<T>> errorSet = validator.validate(obj, groups);
        if (A.isNotEmpty(errorSet)) {
            Multimap<String, String> fieldErrorMap = ArrayListMultimap.create();
            Class<?> clazz = obj.getClass();
            for (ConstraintViolation<?> error : errorSet) {
                if (U.isNotNull(error)) {
                    String field = error.getPropertyPath().toString();
                    fieldErrorMap.put(ValidationUtil.getParamField(clazz, field), error.getMessage());
                }
            }
            Map<String, String> errorMap = ValidationUtil.handleError(fieldErrorMap.asMap());
            if (A.isNotEmpty(errorMap)) {
                throw new ParamException(errorMap);
            }
        }
    }
}
