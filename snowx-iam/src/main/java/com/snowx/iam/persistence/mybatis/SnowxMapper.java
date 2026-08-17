package com.snowx.iam.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.io.Serializable;

public interface SnowxMapper<T, ID extends Serializable> extends BaseMapper<T> {
}
