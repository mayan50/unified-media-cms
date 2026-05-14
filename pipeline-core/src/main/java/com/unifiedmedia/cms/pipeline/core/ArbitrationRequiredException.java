package com.unifiedmedia.cms.pipeline.core;

import lombok.Getter;

/**
 * 仲裁必需异常 - 当刮削结果不唯一时抛出，挂起任务等待人工仲裁
 */
@Getter
public class ArbitrationRequiredException extends Exception {

    private final String nodeName;
    private final Object arbitrationPayload;

    public ArbitrationRequiredException(String nodeName, String message, Object arbitrationPayload) {
        super(message);
        this.nodeName = nodeName;
        this.arbitrationPayload = arbitrationPayload;
    }
}
