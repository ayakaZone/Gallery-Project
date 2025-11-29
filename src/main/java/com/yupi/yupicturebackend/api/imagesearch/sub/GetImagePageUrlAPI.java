package com.yupi.yupicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlAPI {

    /**
     * 获取百度识图以图搜图的接口请求地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // 1.以图识图上传url找到官网获取图片的接口
        // 2.通过接口来获取地址
        // 3.下面是接口的请求参数
        // https://graph.baidu.com/upload?uptime=1764423089731 请求url
        // uptime 1764423089731 查询字符串参数
        // 表单参数
        // image:https%3A%2F%2Fpic.nximg.cn%2Ffile%2F20220830%2F33331825_091955691124_2.jpg
        // tn:pc
        // from:pc
        // image_source:PC_UPLOAD_URL
        /// 1. 准备请求参数 时间戳 和 form表单
        long uptime = System.currentTimeMillis();
        HashMap<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 拼接请求url
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        String acsToken = "1764393702753_1764422564840_9aF9N9QGX1KTxeNL5UhFXV7vO0tIZrIUlF9QGlQUxJ91oHmXKElgANWV6TN3+WoL5k6jmqUYusH" +
                "jmxBZo6MUmot+TPQoXqJoI4WCXIa2DPtPLANRHO1EXJZhH4EJv7WDLh80NLTL/PkiqIRyYonXE7lmxGl9ej1jcSSTu5rIHS3UyeOVd7bxvarkqz08P" +
                "+6rGrosesM7BSWS87txD7ugenMEVbtdelwA6b6yjc4sqyVoD1WtNzjNpBt2yjI+4CPKiDCxX0M5GgnIZqlwD6b+6Rk2to1c6Iq2dZjaWHJ/L8Q6kb5" +
                "CDJVl5eyNDuZfgU1zli9d2g5G3voWBMtMGTLB1j7E64jj+p3V+QrhHZ4R+I3oi5kscHTiSpSz0PYqsMlsIsO/3CPXyXEnuKXFIrWKHE6UOH1e/99Hq" +
                "MUsBTcKHXOQv1f6E1eSwvDEwNmcfVf5yinGn0OLZfbx949uYjZoXWL82Ql5f+Bwrk/k2em0Ztw8yRSl+WGATlkJ5o9dj4rx";
        /// 2. 发送请求
        try {
            HttpResponse response = HttpRequest
                    .post(url).form(formData)
                    .timeout(5000)
                    .header("Acs-Token", acsToken)
                    .execute();
            ThrowUtils.throwIf(response.getStatus() != HttpStatus.HTTP_OK, ErrorCode.OPERATION_ERROR, "发送请求到接口失败");
            // 解析响应
            String body = response.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);
            ThrowUtils.throwIf(result == null || !result.get("status").equals(0), ErrorCode.OPERATION_ERROR, "接口返回数据异常");
            // 获取 data
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            // 识图结果的 url
            String searchResult = data.get("url").toString();
            if (StrUtil.isBlank(searchResult)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口返回数据异常");
            }
            return searchResult;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    public static void main(String[] args) {
        // 测试
        String imageUrl = "https://pic.nximg.cn/file/20220830/33331825_091955691124_2.jpg";
        String imagePageUrl = getImagePageUrl(imageUrl);
        System.out.println(imagePageUrl);
    }
}
