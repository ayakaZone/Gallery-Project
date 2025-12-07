package com.yupi.yupicture.shared.websocket.model;

import com.yupi.yupicture.interfaces.vo.user.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditResponseMessage {

    /**
     * 编辑消息类型
     */
    private String type;

    /**
     * 编辑具体操作
     */
    private String editAction;

    /**
     * 编辑消息
     */
    private String message;

    /**
     * 用户信息
     */
    private UserVO user;

}
