package com.site.material.common;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/** 单据编号生成：前缀-yyyyMMddHHmmss-序号 */
@Component
public class DocNoGenerator {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AtomicInteger seq = new AtomicInteger(0);

    public String next(String prefix) {
        int n = seq.incrementAndGet() % 1000;
        return prefix + "-" + LocalDateTime.now().format(FMT) + "-" + String.format("%03d", n);
    }
}
