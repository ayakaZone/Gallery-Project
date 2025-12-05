package yupicture.infrastructure.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 图片扩展请求参数类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOutPaintingTaskResponse {

    /**
     * 输出图片信息类
     */
    private Output output;

    /**
     * 任务状态枚举（支持字符串映射）
     */
    @Data
    public static class Output {

        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 任务状态
         */
        private String taskStatus;
    }

    /**
     * 错误码
     */
    private String code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 请求唯一标识
     */
    private String resultId;
}



















