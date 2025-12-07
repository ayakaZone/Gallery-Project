package com.yupi.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicture.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.interfaces.dto.picture.*;
import com.yupi.yupicture.interfaces.vo.picture.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Ayaki
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-11-23 10:59:13
 */
public interface PictureApplicationService {

    /**
     * 图片上传
     *
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
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * Picture分页PO转VO
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void pictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

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
     * 用户图片编辑
     *
     * @param picture
     * @param loginUser
     */
    void editPicture(Picture picture, User loginUser);

    /**
     * 用户管理员删除图片
     *
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

    /**
     * 管理员更新图片
     *
     * @param picture
     * @param loginUser
     */
    void updatePicture(Picture picture, User loginUser);

    /**
     * 根据id获取图片
     *
     * @param id
     * @return
     */
    Picture getPictureById(long id);

    /**
     * 获取图片列表
     *
     * @param pictureQueryRequest
     * @return
     */
    Page<Picture> getListPictureByPage(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片列表VO
     *
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    Page<PictureVO> getListPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 获取图片VO
     * @param id
     * @param request
     * @return
     */
    PictureVO getPictureVOById(long id, HttpServletRequest request);

    /**
     * 图片搜索
     * @param searchPictureByPictureRequest
     * @return
     */
    List<ImageSearchResult> searchPictureByPicture(SearchPictureByPictureRequest searchPictureByPictureRequest);

    /**
     * 获取图片列表VO
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    Page<PictureVO> getListPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);
}
