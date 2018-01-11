package com.alibaba.datax.plugin.writer.kafkawriter;

import com.alibaba.datax.common.spi.ErrorCode;

//TODO
public enum KafkaErrorCode implements ErrorCode {

    // only for writer
    WRITE_DATA_ERROR("KafkaErrorCode-01", "往您配置的kafka中写入数据时失败."),
    ;

    private final String code;

    private final String description;

    private KafkaErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s]. ", this.code,
                this.description);
    }
}
