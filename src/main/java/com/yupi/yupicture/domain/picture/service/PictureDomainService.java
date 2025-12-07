package com.yupi.yupicture.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicture.interfaces.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.yupi.yupicture.interfaces.dto.picture.PictureQueryRequest;
import com.yupi.yupicture.interfaces.dto.picture.PictureReviewRequest;
import com.yupi.yupicture.interfaces.vo.picture.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Ayaki
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-11-23 10:59:13
 */
public interface PictureDomainService{

    /**
     * 封装查询条件
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片校验
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
     * 清理COS对象存储中的过期图片
     */
    void clearPictureFile(Picture picture);

    /**
     * 用户图片编辑
     *
     * @param picture
     * @param loginUser
     */
    void editPicture(Picture picture, User loginUser);

    /**
     * 根据颜色相似度查找
     *
     * @param SpaceId
     * @param pictureColor
     * @param login
     * @param space
     * @return
     */
    List<PictureVO> searchPictureByColor(Long SpaceId, String pictureColor, User login, Space space);

    /**
     * 创建图片扩展任务
     *
     * @param request
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest request, User loginUser);

    /**
     * 批量填充图片名称
     *
     * @param pictureList
     * @param nameRule
     */
    void fillPictureWithNameRule(List<Picture> pictureList, String nameRule);

    /**
     * 根据图片ID获取图片
     * @param pictureId
     * @return
     */
    Picture getPictureById(Long pictureId);

    /**
     * 保存图片
     * @param picture
     * @return
     */
    boolean pictureSaveOrUpdate(Picture picture);

    /**
     * 删除图片
     * @param id
     * @return
     */
    boolean removePictureById(Long id);

    /**
     * 批量更新图片
     * @param pictureList
     * @return
     */
    boolean updatePictureBatchById(List<Picture> pictureList);

    /**
     * 批量查询图片
     * @param spaceId
     * @param pictureIdList
     * @return
     */
    List<Picture> pictureLambdaQuery(Long spaceId, List<Long> pictureIdList);

    /**
     * 管理员更新图片
     * @param picture
     * @param loginUser
     */
    void updatePicture(Picture picture, User loginUser);

    /**
     * 获取图片列表
     * @param pictureQueryRequest
     * @return
     */
    Page<Picture> getListPictureByPage(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片列表VO
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    Page<Picture> getListPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 分页
     * @param objectPage
     * @param queryWrapper
     * @return
     */
    Page<Picture> getPage(Page<Picture> objectPage, QueryWrapper<Picture> queryWrapper);
}
