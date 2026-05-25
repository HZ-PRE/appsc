package com.sync.sc.entity;

import java.io.Serializable;


/**
 * @author ld
 * @since 2024-06-13
 */
public class Config implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;

    private String createdAt;

    private String updatedAt;

    private Object val;
    public Config setType(String type) {
        this.type = type;
        return this; // 返回当前对象，支持链式调用
    }

    public Config setVal(Object val) {
        this.val = val;
        return this;
    }
    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getType() {
        return type;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Object getVal() {
        return val;
    }

    @Override
    public String toString() {
        return "Config{type='" + type + "', val='" + val + "'}";
    }

}
