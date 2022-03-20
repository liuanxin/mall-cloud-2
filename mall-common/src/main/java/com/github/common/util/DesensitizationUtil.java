package com.github.common.util;

import com.github.common.Const;

/** 脱敏工具类 */
public final class DesensitizationUtil {

    public static String des(String key, String value) {
        if (U.isNull(key) || U.isNull(value)) {
            return value;
        }

        String lower = key.toLowerCase();
        if (lower.equals(Const.TOKEN.toLowerCase())) {
            return U.foggyToken(value);
        }

        switch (lower) {
            case "password": {
                return "***";
            }
            case "phone": {
                return U.foggyPhone(value);
            }
            case "id-card":
            case "idcard":
            case "id_card": {
                return U.foggyIdCard(value);
            }
            default: {
                return U.foggyValue(value, 1000, 200);
            }
        }
    }
}
