package com.snowx.iam.persistence.mybatis;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.apache.ibatis.session.SqlSession;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SnowxServiceImpl<M extends SnowxMapper<T, ID>, T extends BaseEntity, ID extends Serializable>
        extends ServiceImpl<M, T> implements SnowxService<T, ID> {

    protected M getMapper() {
        return getBaseMapper();
    }

    @Override
    public PageResults<T> fetchPageResults(T entity) {
        return fetchPageResults("queryPageResults", entity);
    }

    @Override
    public PageResults<T> fetchPageResults(String statementId, T entity) {
        String statement = getMapperClass().getName() + "." + statementId;
        List<T> allRows;
        if (getSqlSessionFactory().getConfiguration().hasStatement(statement)) {
            try (SqlSession session = getSqlSessionFactory().openSession()) {
                allRows = session.selectList(statement, entity);
            }
        } else {
            allRows = list(Wrappers.query(entity));
        }

        int page = entity.getPageNumber();
        int pageSize = entity.getPageSize();
        int from = Math.min((page - 1) * pageSize, allRows.size());
        int to = Math.min(from + pageSize, allRows.size());
        List<T> rows = from >= to ? Collections.emptyList() : allRows.subList(from, to);
        return new PageResults<>(page, pageSize, allRows.size(), rows);
    }

    @Override
    public T get(ID id) {
        return getById(id);
    }

    @Override
    public T get(ID id, ID partitionId) {
        return getOne(Wrappers.<T>query().eq("id", id).eq("instid", partitionId), false);
    }

    @Override
    public T get(T entity) {
        return getOne(Wrappers.query(entity), false);
    }

    @Override
    public T get(Wrapper<T> query) {
        return getOne(query, false);
    }

    @Override
    public List<T> query(T entity) {
        return list(Wrappers.query(entity));
    }

    @Override
    public List<T> query(Wrapper<T> query) {
        return list(query);
    }

    @Override
    public List<T> findAll() {
        return list();
    }

    @Override
    public List<T> find(String filter) {
        return find(filter, null, null);
    }

    @Override
    public List<T> find(String filter, Object[] args, int[] argTypes) {
        String condition = normalizeFilter(filter);
        if (condition.isBlank()) {
            return findAll();
        }

        Object[] parameters = args == null ? new Object[0] : args;
        String parameterizedCondition = replacePlaceholders(condition, parameters.length);
        return list(Wrappers.<T>query().apply(parameterizedCondition, parameters));
    }

    @Override
    public T findOne(String filter) {
        return findOne(filter, null, null);
    }

    @Override
    public T findOne(String filter, Object[] args, int[] argTypes) {
        List<T> results = find(filter, args, argTypes);
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    public List<T> findByIds(List<ID> ids) {
        return listByIds(ids);
    }

    @Override
    public T findById(ID id) {
        return getById(id);
    }

    @Override
    public boolean insert(T entity) {
        return save(entity);
    }

    @Override
    public boolean insertBatch(List<T> entities) {
        return saveBatch(entities);
    }

    @Override
    public boolean persist(T entity) {
        return save(entity);
    }

    @Override
    public boolean merge(T entity) {
        return updateById(entity);
    }

    @Override
    public boolean update(T entity) {
        return updateById(entity);
    }

    @Override
    public boolean delete(ID id) {
        return removeById(id);
    }

    @Override
    public boolean deleteById(ID id) {
        return delete(id);
    }

    @Override
    public boolean delete(ID id, ID partitionId) {
        return remove(Wrappers.<T>query().eq("id", id).eq("instid", partitionId));
    }

    @Override
    public boolean delete(Wrapper<T> query) {
        return remove(query);
    }

    @Override
    public boolean deleteBatch(List<ID> ids) {
        return removeByIds(ids);
    }

    @Override
    public boolean deleteBatch(List<ID> ids, ID partitionId) {
        return remove(Wrappers.<T>query().in("id", ids).eq("instid", partitionId));
    }

    @Override
    public boolean existsById(ID id) {
        return getById(id) != null;
    }

    protected int fetchCount(BaseEntity entity, List<?> rows) {
        return rows == null ? 0 : rows.size();
    }

    private String normalizeFilter(String filter) {
        if (filter == null) {
            return "";
        }
        String normalized = filter.trim();
        if (normalized.toLowerCase(Locale.ROOT).startsWith("where ")) {
            return normalized.substring(6).trim();
        }
        return normalized;
    }

    private String replacePlaceholders(String filter, int parameterCount) {
        StringBuilder result = new StringBuilder(filter.length() + parameterCount * 2);
        int parameterIndex = 0;
        for (int i = 0; i < filter.length(); i++) {
            char current = filter.charAt(i);
            if (current == '?' && parameterIndex < parameterCount) {
                result.append('{').append(parameterIndex++).append('}');
            } else {
                result.append(current);
            }
        }
        if (parameterIndex != parameterCount) {
            throw new IllegalArgumentException("SQL placeholder count does not match argument count");
        }
        return result.toString();
    }
}
