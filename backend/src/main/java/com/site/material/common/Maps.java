package com.site.material.common;

import java.util.LinkedHashMap;

/** 有序 Map 构造工具，便于组装 JSON 视图 */
public final class Maps {
    private Maps() {}

    public static LinkedHashMap<String, Object> of(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
