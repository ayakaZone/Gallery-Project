package com.yupi.yupicturebackend.manager.websocket.disruptor;

import com.yupi.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import yupicture.domain.user.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * Disruptor 事件
 */
@Data
public class PictureEditEvent {

    /**
     * 图片编辑请求
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 会话
     */
    private WebSocketSession webSocketSession;

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 用户Id
     */
    private User user;
}
