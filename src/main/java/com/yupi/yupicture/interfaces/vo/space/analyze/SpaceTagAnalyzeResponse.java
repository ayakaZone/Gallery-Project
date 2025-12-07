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
public class SpaceTagAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 使用数量
     */
    private Long count;

}
