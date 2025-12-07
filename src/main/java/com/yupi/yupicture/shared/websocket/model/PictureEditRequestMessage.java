package com.yupi.yupicture.shared.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditRequestMessage {

    /**
     * 编辑消息类型
     */
    private String type;

    /**
     * 编辑具体操作
     */
    private String editAction;
}
