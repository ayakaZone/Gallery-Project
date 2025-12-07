package com.yupi.yupicture.shared.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.yupi.yupicture.shared.websocket.disruptor.PictureEditEventProducer;
import com.yupi.yupicture.shared.websocket.model.PictureEditActionEnum;
import com.yupi.yupicture.shared.websocket.model.PictureEditMessageTypeEnum;
import com.yupi.yupicture.shared.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicture.shared.websocket.model.PictureEditResponseMessage;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * key：图片Id
     * value：图片对应的 session 集合
     */
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * key：图片Id
     * value：图片正在编辑的用户Id
     */
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();


    /**
     * 建立连接成功后
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 获取参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 给当前图片ID初始化空Set集合
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        // 保存用户连接到编辑当前图片的用户会话集合
        pictureSessions.get(pictureId).add(session);
        // 构造响应消息，为了提示某用户与当前图片建立了会话连接
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户：%s 加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
        // 广播给编辑当前图片的所有用户会话
        this.broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 收到消息之后
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 解析 JsonStr 编辑消息
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();
        // 获得消息枚举类型
        PictureEditMessageTypeEnum messageTypeEnum = PictureEditMessageTypeEnum.valueOf(type);
        // 从 Session 中获取参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 调用 Disruptor 生产者生产事件，触发 Disruptor 消费者消费事件，完成图片编辑消息的处理
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }

    /**
     * 关闭连接
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 获取参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 用户关闭连接时，移除正在编辑图片的用户编辑状态
        this.handleExitEditMessage(null, session, user, pictureId);
        // 关闭该用户与该图片连接状态
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (webSocketSessions != null) {
            // 关闭连接
            webSocketSessions.remove(session);
            // 如果该图片无人编辑，移除该图片id
            if (webSocketSessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
            log.info("用户关闭连接");
        }
    }

    /**
     * 进入编辑图片
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        // 一张图片只能有一个用户编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置当前用户为编辑状态
            pictureEditingUsers.put(pictureId, user.getId());
            // 构造响应信息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户：%s 开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
            // 广播给编辑当前图片的所有用户会话
            this.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 编辑图片
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        // 校验编辑操作类型
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum editActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (ObjUtil.isEmpty(editActionEnum)) {
            return;
        }
        // 校验当前用户是否是图片正在编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (ObjUtil.isNotEmpty(editingUserId) && editingUserId.equals(user.getId())) {
            // 构造编辑的响应消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
            String message = String.format("用户：%s 执行 %s 操作", user.getUserName(), editActionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            // 广播给排除自己以外编辑当前图片的所有用户会话
            this.broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 退出编辑图片
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (ObjUtil.isNotEmpty(editingUserId) && editingUserId.equals(user.getId())) {
            // 删除当前用户
            pictureEditingUsers.remove(pictureId);
            // 构造退出编辑的响应消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户：%s 退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userApplicationService.getUserVO(user));
            // 广播给编辑当前图片的所有用户会话
            this.broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 广播编辑图片的消息给所有用户
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession             排除的会话
     * @throws IOException
     */
    private void broadcastToPicture
    (Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws IOException {

        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(webSocketSessions)) {
            // 解决JsonStr 转 String 包含Long类型导致精度丢失问题
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            // 使用Jackson 序列化转换JsonStr
            String messageStr = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(messageStr);
            // 遍历编辑当前图片的所有用户会话
            for (WebSocketSession webSocketSession : webSocketSessions) {
                // 排除指定的用户会话
                if (ObjUtil.isNotEmpty(excludeSession) && webSocketSession.equals(excludeSession)) {
                    continue;
                }
                // 检查会话是否处于打开状态
                if (webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播编辑图片的消息给所有用户，不排除任何用户会话
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @throws IOException
     */
    private void broadcastToPicture
    (Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}














