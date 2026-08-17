package com.snowx.iam.persistence.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

public class Query<T> extends QueryWrapper<T> {
    public static <T> Query<T> builder() {
        return new Query<>();
    }

    @Override
    public Query<T> eq(String column, Object value) {
        super.eq(column, value);
        return this;
    }
}
