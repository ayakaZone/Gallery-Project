package com.yupi.yupicture.infrastructure.api.aliyunai.model;


import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 图片扩展请求参数类
 */
@Data
public class CreateOutPaintingTaskRequest implements Serializable {
    /**
     * 模型
     */
    private String model = "image-out-painting";
    /**
     * 输入图像信息
     */
    private Input input;
    /**
     * 图片处理参数
     */
    private Parameters parameters;

    @Data
    public static class Input {

        /**
         * 图片url
         */
        @Alias("image_url")
        private String imageUrl;
    }

    @Data
    public static class Parameters implements Serializable {

        /**
         * 逆时针旋转角度
         */
        private Integer angle;

        /**
         * 图片宽高比
         */
        @Alias("output_radio")
        private String outputRadio;

        /**
         * 图像居中，在水平方向按比例扩展
         */
        @JsonProperty("xScale")
        @Alias("x_scale")
        private Double xScale;

        /**
         * 图像居中，在垂直方向按比例扩展
         */
        @JsonProperty("yScale")
        @Alias("y_scale")
        private Double yScale;

        /**
         * 图片上方增加像素
         */
        @Alias("top_offset")
        private Integer topOffset;

        /**
         * 图片下方增加像素
         */
        @Alias("bottom_offset")
        private Integer bottomOffset;

        /**
         * 图片左边增加像素
         */
        @Alias("left_offset")
        private Integer leftOffset;

        /**
         * 图片右边增加像素
         */
        @Alias("right_offset")
        private Integer rightOffset;

        /**
         * 图片最佳质量
         */
        @Alias("best_quality")
        private Boolean bestQuality;

        /**
         * 图片大小限制
         */
        @Alias("limit_image_size")
        private Boolean limitImageSize;

        /**
         * AI 水印(非商用就不加水印了)
         */
        @Alias("add_watermark")
        private Boolean addWatermark = false;
    }
}



















