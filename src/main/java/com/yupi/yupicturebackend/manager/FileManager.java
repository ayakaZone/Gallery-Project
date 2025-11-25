package com.yupi.yupicturebackend.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.yupi.yupicturebackend.exception.ErrorCode.PARAMS_ERROR;


@Slf4j
@Service
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;
    @Resource
    private CosManager cosManager;

    /**
     * 通用图片上传 Url
     *
     * @param fileUrl
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {

        // todo url格式校验
        this.validPicture(fileUrl);
        // 图片校验
        // this.validPicture(multipartFile);

        /// 生成上传文件地址
        // 获取文件原始名
        // String originalFilename = multipartFile.getOriginalFilename();
        // 获取后缀
        // String suffix = FileUtil.getSuffix(originalFilename);
        // todo url 获取原始文件名
        // 获取文件原始名
        String originFileName = FileUtil.mainName(fileUrl);
        // 获得后缀
        String suffix = FileUtil.getSuffix(originFileName);
        // 定义前缀 时间戳 + 16位随机数
        String date = DateUtil.formatDate(new Date());
        String randomNum = RandomUtil.randomString(16);
        // 拟定上传地址
        String fileName = String.format("%s_%s.%s", date, randomNum, suffix);
        // 带前缀的上传地址
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, fileName);
        ///  上传文件
        // 获得文件对象
        File file = null;
        try {
            // 生成临时文件
            file = File.createTempFile(uploadPath, null);
            // todo url 下载图片并上传
            HttpUtil.downloadFile(fileUrl, file);
            // 将真正的文件写入临时文件
            // multipartFile.transferTo(file);
            // 上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 解析文件数据
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 包装图片信息类
            String format = imageInfo.getFormat(); // 格式
            int picWidth = imageInfo.getWidth();   // 宽
            int picHeight = imageInfo.getHeight(); // 高
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue(); // 宽高比
            // 包装
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(format);
            // 返回图片的详细信息
            return uploadPictureResult;
        } catch (Exception e) {
            // 异常捕获
            log.error("file upload error, filePath", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            // 清理临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 通用图片上传
     *
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 图片校验
        this.validPicture(multipartFile);

        /// 生成上传文件地址
        // 获取文件原始名
        String originalFilename = multipartFile.getOriginalFilename();
        // 获取后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 定义前缀 时间戳 + 16位随机数
        String date = DateUtil.formatDate(new Date());
        String randomNum = RandomUtil.randomString(16);
        // 拟定上传地址
        String fileName = String.format("%s_%s.%s", date, randomNum, suffix);
        // 带前缀的上传地址
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, fileName);
        ///  上传文件
        // 获得文件对象
        File file = null;
        try {
            // 生成临时文件
            file = File.createTempFile(uploadPath, null);
            // 将真正的文件写入临时文件
            multipartFile.transferTo(file);
            // 上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 解析文件数据
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 包装图片信息类
            String format = imageInfo.getFormat(); // 格式
            int picWidth = imageInfo.getWidth();   // 宽
            int picHeight = imageInfo.getHeight(); // 高
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue(); // 宽高比
            // 包装
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(format);
            // 返回图片的详细信息
            return uploadPictureResult;
        } catch (Exception e) {
            // 异常捕获
            log.error("file upload error, filePath", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            // 清理临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 清理临时文件
     *
     * @param file
     */
    private static void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean result = file.delete();
        if (!result) {
            log.error("file delete error, filePath: {}", file.getAbsoluteFile());
        }
    }

    /**
     * 图片校验
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
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
     * 图片校验 Url
     *
     * @param
     */
    private void validPicture(String fileUrl) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(fileUrl), PARAMS_ERROR);
        // Url 格式校验
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(PARAMS_ERROR);
        }
        // Url 协议校验
        ThrowUtils.throwIf(!fileUrl.startsWith("htttp://") || !fileUrl.startsWith("htttps://"), PARAMS_ERROR, "仅支持HTTP或HTTPS协议的文件地址");
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
}
