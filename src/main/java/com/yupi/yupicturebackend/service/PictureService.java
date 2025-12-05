package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import yupicture.domain.user.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Ayaki
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-11-23 10:59:13
 */
public interface PictureService extends IService<Picture> {

    /**
     * 图片上传
     * @param inputSource
     * @param pictureUploadRequest
     * @param user
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User user);

    /**
     * 封装查询条件
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * Picture PO 转 VO
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * Picture分页PO转VO
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片校验
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void    pictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 清理COS对象存储中的过期图片
     */
    void clearPictureFile(Picture picture);

    /**
     * 图片操作权限校验——已使用Sa-Token完成权限校验
     * @param user
     * @param picture
     */
/*    void checkPictureAuth(User user, Picture picture);*/

    /**
     * 用户图片编辑
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 用户管理员删除图片
     * @param deleteRequest
     * @param loginUser
     */
    void deletePicture(DeleteRequest deleteRequest, User loginUser);

    /**
     * 根据颜色相似度查找
     *
     * @param SpaceId
     * @param pictureColor
     * @param login
     */
    List<PictureVO> searchPictureByColor(Long SpaceId, String pictureColor, User login);

    /**
     * 批量修改图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建图片扩展任务
     *
     * @param request
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest request, User loginUser);
}
