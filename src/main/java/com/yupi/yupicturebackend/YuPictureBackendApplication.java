package com.yupi.yupicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class}) // 禁用 ShardingSphere 的自动配置
@MapperScan("com.yupi.yupicturebackend.mapper") // MyBatisPlus 扫描 Mapper 包
@EnableAspectJAutoProxy(exposeProxy = true) // 启用 AOP 暴露代理
@EnableAsync // 启用异步处理
public class YuPictureBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(YuPictureBackendApplication.class, args);
    }

}
