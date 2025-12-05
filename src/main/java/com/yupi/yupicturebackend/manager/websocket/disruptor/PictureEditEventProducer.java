package com.yupi.yupicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.yupi.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import yupicture.domain.user.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Component
public class PictureEditEventProducer {

    @Resource
    Disruptor<PictureEditEvent> disruptor;
    /**
     * 事件生产者
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId){
        // 获得 ringBuffer 环形队列
        RingBuffer<PictureEditEvent> ringBuffer = disruptor.getRingBuffer();
        // 获取下一个可用的序号
        long next = ringBuffer.next();
        // 获取该序号对应的事件对象并设置参数（生产消息）
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setWebSocketSession(session);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureId(pictureId);
        // 发布事件
        ringBuffer.publish(next);
    }

    /**
     * 优雅停机
     */
    @PreDestroy
    public void destroy() {
        disruptor.shutdown();
    }
}
