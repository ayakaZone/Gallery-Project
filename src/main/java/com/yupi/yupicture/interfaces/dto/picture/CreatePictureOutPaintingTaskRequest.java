package com.yupi.yupicture.interfaces.dto.picture;

import lombok.Data;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}
