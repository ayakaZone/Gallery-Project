package com.yupi.yupicture.interfaces.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 时间维度
     */
    private String timeDimension;
}
