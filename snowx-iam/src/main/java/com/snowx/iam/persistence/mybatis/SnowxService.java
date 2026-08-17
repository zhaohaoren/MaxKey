package com.snowx.iam.persistence.mybatis;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.spring.service.IService;

import java.io.Serializable;
import java.util.List;

public interface SnowxService<T, ID extends Serializable> extends IService<T> {
    PageResults<T> fetchPageResults(T entity);

    PageResults<T> fetchPageResults(String statementId, T entity);

    T get(ID id);

    T get(ID id, ID partitionId);

    T get(T entity);

    T get(Wrapper<T> query);

    List<T> query(T entity);

    List<T> query(Wrapper<T> query);

    List<T> findAll();

    List<T> find(String filter);

    List<T> find(String filter, Object[] args, int[] argTypes);

    T findOne(String filter);

    T findOne(String filter, Object[] args, int[] argTypes);

    List<T> findByIds(List<ID> ids);

    T findById(ID id);

    boolean insert(T entity);

    boolean insertBatch(List<T> entities);

    boolean persist(T entity);

    boolean merge(T entity);

    boolean update(T entity);

    boolean delete(ID id);

    boolean deleteById(ID id);

    boolean delete(ID id, ID partitionId);

    boolean delete(Wrapper<T> query);

    boolean deleteBatch(List<ID> ids);

    boolean deleteBatch(List<ID> ids, ID partitionId);

    boolean existsById(ID id);
}
