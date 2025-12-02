package com.yupi.yupicturebackend.model.vo.space.analyze;

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
public class SpaceUserAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 时间段
     */
    private String period;

    /**
     * 上传文件数
     */
    private Long count;
}
