package com.yupi.yupicturebackend.model.dto.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceCategoryAnalyzeRequest extends SpaceAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = -1L;

}
