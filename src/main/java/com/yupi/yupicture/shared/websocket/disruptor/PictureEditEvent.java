package com.yupi.yupicture.shared.websocket.disruptor;

import com.yupi.yupicture.shared.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicture.domain.user.entity.User;
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
