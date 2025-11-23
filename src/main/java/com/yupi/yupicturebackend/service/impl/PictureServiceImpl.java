package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.FileManager;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author Ayaki
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-11-23 10:59:13
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FileManager fileManager;
    /**
     * 文件上传
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param user
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User user) {
        /// 校验
        ThrowUtils.throwIf(user.getUserRole() == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是更新还是新增
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 图片有id再判断有没有数据
        if (pictureId != null) {
            boolean exists = lambdaQuery().eq(Picture::getId, pictureId).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        /// 上传图片
        // 公共上传路径
        String uploadPathPrefix = String.format("public/%s", user.getId());
        // 上传图片并返回信息
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // 构建图片信息类
        Picture picture = BeanUtil.copyProperties(uploadPictureResult, Picture.class);
        // 坑!!! 表picture 的字段为 name, 表 UploadPictureResult 的字段为 picName 需要手动映射
        picture.setName(uploadPictureResult.getPicName());
        picture.setUserId(user.getId());
        /// 插入数据库(更新/新增)
        if (pictureId != null) {
            // 更新需要添加字段
            picture.setId(pictureId); // 图片id
            picture.setEditTime(new Date()); // 修改时间
        }

        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
        /// 封装VO对象
        return PictureVO.objToVo(picture);
    }
}




