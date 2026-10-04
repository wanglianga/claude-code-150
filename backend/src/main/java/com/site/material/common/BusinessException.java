package com.site.material.common;

/** 业务校验失败 -> HTTP 400 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
