package com.snowx.iam.persistence.mybatis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.snowx.iam.id.generator.IdGeneratorFactory;

import java.io.Serializable;

public class BaseEntity implements Serializable {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 1000;

    @JsonIgnore
    @TableField(exist = false)
    protected String pageSelectId;
    @JsonIgnore
    @TableField(exist = false)
    protected boolean pageable;
    @JsonIgnore
    @TableField(exist = false)
    protected int pageSize = DEFAULT_PAGE_SIZE;
    @JsonIgnore
    @TableField(exist = false)
    protected int pageNumber = 1;
    @JsonIgnore
    @TableField(exist = false)
    protected String sortOrder;
    @JsonIgnore
    @TableField(exist = false)
    protected String sortKey;
    @JsonIgnore
    @TableField(exist = false)
    protected String orderBy;
    @JsonIgnore
    @TableField(exist = false)
    protected int rows;
    @JsonIgnore
    @TableField(exist = false)
    protected int startRow;
    @JsonIgnore
    @TableField(exist = false)
    protected int endRow;

    public String generateId() {
        return new IdGeneratorFactory().generate();
    }

    public void build() {
        pageable = true;
        calculate();
    }

    protected void calculate() {
        startRow = (pageNumber - 1) * pageSize;
        endRow = startRow + pageSize;
    }

    public String loggingEvent() {
        return "";
    }

    public String loggingEventTargetId() {
        return "";
    }

    public String loggingEventTargetName() {
        return "";
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = Math.max(rows, 0);
        setPageSize(rows);
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = Math.max(pageNumber, 1);
        calculate();
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        calculate();
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = Math.max(startRow, 0);
        this.pageNumber = this.startRow / pageSize + 1;
        calculate();
    }

    public int getEndRow() {
        return endRow;
    }

    public void setEndRow(int endRow) {
        this.endRow = Math.max(endRow, startRow);
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public boolean isPageable() {
        return pageable;
    }

    public void setPageable(boolean pageable) {
        this.pageable = pageable;
    }

    public void setPageable() {
        this.pageable = true;
    }

    public String getPageSelectId() {
        return pageSelectId;
    }

    public void setPageSelectId(String pageSelectId) {
        this.pageSelectId = pageSelectId;
    }
}
