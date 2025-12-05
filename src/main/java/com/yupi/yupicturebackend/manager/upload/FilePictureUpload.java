package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import yupicture.infrastructure.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static yupicture.infrastructure.exception.ErrorCode.PARAMS_ERROR;

@Service
public class FilePictureUpload extends PictureUploadTemplate {

    /**
     * 校验 MultipartFile
     *
     * @param inputSource
     */
    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(ObjUtil.isEmpty(multipartFile), PARAMS_ERROR);
        // 大小不能超过 2m
        final long ONE_M = 1024 * 1024;
        long size = multipartFile.getSize();
        ThrowUtils.throwIf(size > 2 * ONE_M, PARAMS_ERROR, "图片大小不能超过 2MB");
        // 图片后缀格式校验
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "webp");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(suffix), PARAMS_ERROR, "文件类型有误");
    }

    /**
     * MultipartFile 获取文件原始名
     *
     * @param inputSource
     * @return
     */
    @Override
    protected String getFileName(Object inputSource) {
        return ((MultipartFile)inputSource).getOriginalFilename();
    }

    /**
     * 从 MultipartFile 下载到临时文件
     *
     * @param inputSource
     * @param file
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception{
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
