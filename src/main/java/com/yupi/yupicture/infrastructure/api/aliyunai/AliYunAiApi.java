package com.yupi.yupicture.infrastructure.api.aliyunai;/*
package com.yupi.yupicturebackend.api.aliyunai;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class AliYunApi {

    // 读取配置文件
    @Value("${AliYunAI.apiKey}")
    private String apiKey;

    // 创建任务地址
    final String CREATE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务结果地址
    final String GET_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    */
/**
     * 请求参数示例
     * curl --location --request POST 'https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting' \
     * --header "Authorization: Bearer $DASHSCOPE_API_KEY" \
     * --header 'X-DashScope-Async: enable' \
     * --header 'Content-Type: application/json' \
     * --data '{
     * "model": "image-out-painting",
     * "input": {
     * "image_url": "http://xxx/image.jpg"
     * },
     * "parameters":{
     * "angle": 45,
     * "x_scale":1.5,
     * "y_scale":1.5
     * }
     * }'
     *//*



    */
/**
     * 创建图片扩展任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     *//*

    public CreateOutPaintingTaskResponse createTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        // 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty
                (createOutPaintingTaskRequest), ErrorCode.PARAMS_ERROR, "扩图参数不能为空");
        // 创建任务请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer" + apiKey)
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        // 发送创建任务的请求
        try (HttpResponse httpResponse = httpRequest.execute()) { // 捕获结束自动释放
            // 校验请求状态
            if (!httpResponse.isOk()) {
                log.error("创建任务失败 {}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建任务失败");
            }
            // 解析响应结果
            CreateOutPaintingTaskResponse response = JSONUtil.toBean
                    (httpResponse.body(), CreateOutPaintingTaskResponse.class);
            // 校验响应结果
            CreateOutPaintingTaskResponse.Output output = response.getOutput();
            String errorCode = output.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String message = output.getMessage();
                log.error("创建任务失败 错误码:{}, 错误消息:{}", errorCode, message);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建任务失败");
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    */

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;

/**
     *
     * 查询图片扩展任务结果
     *
     * @param
     * @return
     *//*

    public GetOutPaintingTaskResponse getTask(String taskId) {
        // 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(taskId), ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        // 发送查询任务请求
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer" + apiKey)
                .execute()) {
            // 校验
            if (!httpResponse.isOk()) {
                log.error("查询任务失败 {}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询任务失败");
            }
            // 解析数据并返回
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


*/
@Slf4j
@Component
public class AliYunAiApi {
    // 读取配置文件
    @Value("${AliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为enable。
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
