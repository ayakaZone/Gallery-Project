package com.yupi.yupicture.shared.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * Disruptor 配置类
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    @Bean
    public Disruptor<PictureEditEvent> messageModelRingBuffer(){
        // 定义 RingBuffer （环形队列）的大小
        int ringBufferSize = 1024 * 256;
        // 初始化 Disruptor
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,
                ringBufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("PictureEditEventDisruptor").build()
        );
        // 配置 Disruptor 的事件处理器
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        // 启动 Disruptor
        disruptor.start();
        return disruptor;

    }
}
