package com.yupi.yupicture.shared.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.yupi.yupicture.shared.websocket.PictureEditHandler;
import com.yupi.yupicture.shared.websocket.model.PictureEditMessageTypeEnum;
import com.yupi.yupicture.shared.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicture.shared.websocket.model.PictureEditResponseMessage;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.domain.user.entity.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * Disruptor 事件处理器(消费者)
 */
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 事件处理器
     *
     * @param pictureEditEvent
     * @throws Exception
     */
    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        // 解析事件参数
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getWebSocketSession();
        Long pictureId = pictureEditEvent.getPictureId();
        User user = pictureEditEvent.getUser();
        // 消息类型枚举类型
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum messageTypeEnum = PictureEditMessageTypeEnum.valueOf(type);
        // 根据消息类型做不同处理
        switch (messageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                PictureEditResponseMessage errorResponseMessage = new PictureEditResponseMessage();
                errorResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                errorResponseMessage.setMessage("消息类型错误");
                errorResponseMessage.setUser(userApplicationService.getUserVO(user));
                // 对当前用户会话发送请求错误消息
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(errorResponseMessage)));
        }
    }
}
