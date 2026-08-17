package com.snowx.iam.persistence.mybatis;

import java.util.List;

public class PageResults<T> {
    private int page;
    private int total;
    private int totalPage;
    private long records;
    private List<T> rows;

    public PageResults() {
    }

    public PageResults(int page, int pageSize, long records, List<T> rows) {
        this.page = page;
        this.records = records;
        this.rows = rows;
        this.total = rows == null ? 0 : rows.size();
        this.totalPage = pageSize <= 0 ? 0 : (int) ((records + pageSize - 1) / pageSize);
    }

    public PageResults(int page, int pageSize, int total, long records, List<T> rows) {
        this(page, pageSize, records, rows);
        this.total = total;
    }

    public static Integer parseRecords(List<?> rows) {
        return rows == null ? 0 : rows.size();
    }

    public static Integer parseCount(Object value) {
        return value == null ? 0 : Integer.valueOf(value.toString());
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public long getRecords() {
        return records;
    }

    public void setRecords(long records) {
        this.records = records;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }
}
