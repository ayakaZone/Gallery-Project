package com.yupi.yupicture.interfaces.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间使用分析响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceCategoryAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 分类
     */
    private String category;

    /**
     * 数量
     */
    private Long count;

    /**
     * 图片总大小
     */
    private Long totalSize;

}
