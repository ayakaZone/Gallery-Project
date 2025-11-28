package com.yupi.yupicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.qcloud.cos.model.ciModel.persistence.Rule;
import com.yupi.yupicturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 通用文件上传 COS
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 通用文件下载 COS
     *
     * @param key
     * @return
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /// 优化图片转 webp 格式上传方式 1. 原图上传 2. 转 webp 上传 防止原图后缀名丢失

    /**
     * 通用图片上传 COS
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        /// 上传原图
        // 文件操作请求配置
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 图片操作类
        PicOperations picOperations = new PicOperations();
        // 配置需要返回图片信息
        picOperations.setIsPicInfo(1);
        // 对图片操作的规则
        List<PicOperations.Rule> ruleList = new ArrayList<>();
        // 1. 更改文件名为 .webp
        String webpKey = FileUtil.mainName(key) + ".webp";
        // 更改图片格式为 webp 规则
        PicOperations.Rule webpRule = new PicOperations.Rule();
        webpRule.setRule("imageMogr2/format/webp"); // 更改图片格式
        webpRule.setBucket(cosClientConfig.getBucket()); // 存储桶
        webpRule.setFileId(webpKey); // 文件名
        // 添加规则
        ruleList.add(webpRule);
        // 2. 原图缩略图规则，文件大于 20kb 才做缩略图处理
        if (file.length() > 2 * 1024) {
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256)); // 缩略图规则
            thumbnailRule.setBucket(cosClientConfig.getBucket()); // 存储桶
            thumbnailRule.setFileId(thumbnailKey); // 文件名
            // 添加规则
            ruleList.add(thumbnailRule);
        }
        picOperations.setRules(ruleList);
        // 将图片配置存入文件操作请求中
        putObjectRequest.setPicOperations(picOperations);
        // 通过 COS提供的 cosClient 上传图片
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) throws CosClientException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

}
