package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.yupi.yupicturebackend.exception.ErrorCode.PARAMS_ERROR;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {

    /**
     * 校验 URL
     *
     * @param inputSource
     */
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(ObjUtil.isEmpty(fileUrl), PARAMS_ERROR, "文件地址不能为空");
        // Url 格式校验
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(PARAMS_ERROR);
        }
        // Url 协议校验
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), PARAMS_ERROR, "仅支持HTTP或HTTPS协议的文件地址");
        HttpResponse httpResponse = null;
        try {
            // 发送 HEAD 请求验证文件是否存在
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 判断状态
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 校验类型
            String contentType = httpResponse.header("Content-type");
            // 是否为空
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()), PARAMS_ERROR, "文件类型错误");
            }
            // 校验文件大小
            String contentLengthStr = httpResponse.header("content-length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > 2 * ONE_M, PARAMS_ERROR, "图片大小不能超过 2MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            // 释放资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
    }

    /**
     * URl 获取文件原始名
     *
     * @param inputSource
     * @return
     */
    @Override
    protected String getFileName(Object inputSource) {
        return FileUtil.getName((String) inputSource);
    }

    /**
     * 从 URL 下载到临时文件
     *
     * @param inputSource
     * @param file
     */
    @Override
    protected void processFile(Object inputSource, File file) {
        HttpUtil.downloadFile((String) inputSource, file);
    }
}
