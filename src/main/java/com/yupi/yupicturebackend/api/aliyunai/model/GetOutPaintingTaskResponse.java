package com.yupi.yupicturebackend.api.aliyunai.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 查询图片扩展结果响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetOutPaintingTaskResponse implements Serializable {
    /**
     * 请求唯一标识符，由系统生成。
     * 可用于请求追踪、问题排查、客服支持等场景。
     * 推荐在日志中打印此 ID 以便定位具体请求。
     */
    private String requestId;

    private OutPut output;

    @Data
    public static class OutPut {

        /**
         * 任务唯一标识 ID。
         * 用于查询任务结果，有效期为 24 小时。
         * 示例值："task-xxxxx-yyyyy-zzzzz"
         */
        private String taskId;

        /**
         * 当前任务的状态。
         * 枚举值：
         * - PENDING：任务排队中
         * - RUNNING：任务处理中
         * - SUCCEEDED：任务执行成功
         * - FAILED：任务执行失败
         * - CANCELED：任务已取消
         * - UNKNOWN：任务不存在或状态未知
         */
        private String taskStatus;

        /**
         * 任务提交时间，ISO 8601 格式的时间戳（UTC）。
         * 示例值："2025-04-05T08:30:15Z"
         */
        private String submitTime;

        /**
         * 调度时间
         */
        private String scheduledTime;

        /**
         * 任务完成时间（仅当任务结束时存在），ISO 8601 格式的时间戳（UTC）。
         * 示例值："2025-04-05T08:35:22Z"
         */
        private String endTime;

        /**
         * 输出图像的公网可访问 URL 地址。
         * 只有当 task_status 为 SUCCEEDED 时才返回。
         * 该链接有一定时效性，请及时下载保存。
         */
        private String outputImageUrl;

        /**
         * 请求失败时的错误码。
         * 成功时不返回此字段。
         * 常见错误码参考文档：https://help.aliyun.com/document_detail/xxx.html
         */
        private String code;

        /**
         * 请求失败时的详细错误描述信息。
         * 成功时不返回此字段。
         * 可用于定位问题原因，建议记录到日志中。
         */
        private String message;

        /**
         * 任务指标信息。
         */
        private TaskMetrics taskMetrics;
    }

    @Data
    private static class TaskMetrics {

        /**
         * 总任务数
         */
        private Integer total;

        /**
         * 成功任务数
         */
        private Integer succeeded;

        /**
         * 失败任务数
         */
        private Integer failed;
    }
}




















