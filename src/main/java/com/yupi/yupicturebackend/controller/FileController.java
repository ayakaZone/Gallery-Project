package com.yupi.yupicturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.manager.CosManager;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@Slf4j
@Api(tags = "文件接口")
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;



    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart MultipartFile multipartFile) {
        // 文件目录
        // 文件原始名称
        String originalFilename = multipartFile.getOriginalFilename();
        // 拼接文件路径
        String filePath = String.format("test/%s", originalFilename);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(filePath, null);
            // 将 Spring MVC 接收的文件写入临时文件
            multipartFile.transferTo(file);
            // 将临时文件上传到 COS
            cosManager.putObject(filePath, file);
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload error, filePath: {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            if (file != null) {
                boolean result = file.delete();
                if (!result) {
                    log.error("file delete error, filePath: {}", filePath);
                }
            }
        }
    }

    /**
     * 测试文件上传
     *
     * @param filePath
     * @param response
     */
    @PostMapping("/test/download")
    @AuthCheck(mustRole = "admin")
    public void testDownloadFile(String filePath, HttpServletResponse response) throws IOException {
        COSObjectInputStream objectContent = null;
        try {
            // 获取文件
            COSObject cosObject = cosManager.getObject(filePath);
            objectContent = cosObject.getObjectContent();
            // 获取文件字节数组
            byte[] byteArray = IOUtils.toByteArray(objectContent);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filePath);
            // 写入响应
            response.getOutputStream().write(byteArray);
            // 刷新
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file upload error, filePath: {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
        } finally {
            if (objectContent != null) {
                objectContent.close();
            }
        }
    }
}
