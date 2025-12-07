package com.yupi.yupicture.interfaces.vo.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间使用分析响应类
 */
@Data
public class SpaceUsageAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 已使用数量
     */
    private Long usedCount;

    /**
     * 已使用空间
     */
    private Long usedSize;

    /**
     * 最大数量
     */
    private Long maxCount;

    /**
     * 最大空间
     */
    private Long maxSize;

    /**
     * 使用大小占比
     */
    private Double sizeUsageRatio;

    /**
     * 使用数量占比
     */
    private Double countUsageRatio;
}
