package yupicture.infrastructure.common;

import lombok.Data;

/**
 * 通用分页请求类
 */
@Data
public class PageRequest {

    /**
     * 页码
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序
     */
    private String sortOrder = "descend";
}
