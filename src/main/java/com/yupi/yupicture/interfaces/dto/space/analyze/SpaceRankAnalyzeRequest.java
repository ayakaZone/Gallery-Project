package com.yupi.yupicture.interfaces.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceRankAnalyzeRequest extends SpaceAnalyzeRequest implements Serializable {

    /**
     * 用户空间排行数
     */
    private Integer TopN;

    private static final long serialVersionUID = -1L;

}
