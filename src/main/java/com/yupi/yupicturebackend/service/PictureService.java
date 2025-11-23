package com.yupi.yupicturebackend.service;

import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Ayaki
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-11-23 10:59:13
 */
public interface PictureService extends IService<Picture> {

    /**
     * 图片上传
     * @param multipartFile
     * @param pictureUploadRequest
     * @param user
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User user);
}
