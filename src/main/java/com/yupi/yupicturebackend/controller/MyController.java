package com.yupi.yupicturebackend.controller;

import yupicture.infrastructure.common.BaseResponse;
import yupicture.infrastructure.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MyController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<?> health(){
        return ResultUtils.success("ok");
    }
}
