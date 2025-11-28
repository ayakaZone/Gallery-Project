package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
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
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
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
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 通用图片上传 Url
     *
     * @param inputSource
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {

        // todo 格式校验
        validPicture(inputSource);
        // todo 获取原始文件名
        String originFileName = getFileName(inputSource);
        // 清除后缀多余的字符
        String cleanFileName = originFileName.split("[?#]")[0];
        // 获得后缀
        String suffix = FileUtil.getSuffix(cleanFileName);
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
            // todo 下载图片到临时文件
            processFile(inputSource, file);
            // 上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 解析文件数据
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 根据规则处理后的结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject webpObject = objectList.get(0);
                // 缩略图默认为 webp格式
                CIObject thumbnailObject = webpObject;
                // 判断是否配置了缩略图
                if (objectList.size() > 1) {
                    thumbnailObject = objectList.get(1);
                }
                return buildResult(originFileName, webpObject, thumbnailObject);
            }
            // todo 封装返回结果
            return buildResult(imageInfo, uploadPath, originFileName, file);
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
     * 处理文件
     * @param inputSource
     * @param file
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 获得文件原始名
     * @param inputSource
     * @return
     */

    protected abstract String getFileName(Object inputSource);

    /**
     * 校验输入源（url/MultipartFile）
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 封装返回结果（带图片处理后）
     * @param originFileName
     * @param webpObject
     * @return
     */
    private UploadPictureResult buildResult(String originFileName, CIObject webpObject, CIObject thumbObject) {
        // 包装图片信息类

        String format = webpObject.getFormat(); // 格式
        int picWidth = webpObject.getWidth();   // 宽
        int picHeight =  webpObject.getHeight(); // 高
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue(); // 宽高比
        // 包装
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + webpObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
        uploadPictureResult.setPicSize(webpObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" +thumbObject.getKey());
        // 返回图片的详细信息
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     * @param imageInfo
     * @param uploadPath
     * @param originFileName
     * @param file
     * @return
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String uploadPath, String originFileName, File file) {
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
}
