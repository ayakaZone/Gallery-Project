package com.yupi.yupicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yupicturebackend.manager.auth.SpaceUserAuthManager;
import com.yupi.yupicturebackend.manager.auth.StpKit;
import com.yupi.yupicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.yupi.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupicturebackend.model.entity.Space;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yupicture.application.service.PictureApplicationService;
import yupicture.application.service.UserApplicationService;
import yupicture.domain.picture.entity.Picture;
import yupicture.domain.user.constant.UserConstant;
import yupicture.domain.user.entity.User;
import yupicture.infrastructure.annotation.AuthCheck;
import yupicture.infrastructure.api.aliyunai.AliYunAiApi;
import yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import yupicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import yupicture.infrastructure.api.imagesearch.ImageSearchApiFacade;
import yupicture.infrastructure.api.imagesearch.model.ImageSearchResult;
import yupicture.infrastructure.common.BaseResponse;
import yupicture.infrastructure.common.DeleteRequest;
import yupicture.infrastructure.common.ResultUtils;
import yupicture.infrastructure.exception.ErrorCode;
import yupicture.infrastructure.exception.ThrowUtils;
import yupicture.interfaces.assembler.PictureAssembler;
import yupicture.interfaces.dto.picture.*;
import yupicture.interfaces.vo.picture.PictureTagCategory;
import yupicture.interfaces.vo.picture.PictureVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static yupicture.domain.picture.valueobject.PictureReviewStatusEnum.PASS;

@RestController
@RequestMapping("/picture")
@Slf4j
@Api(tags = "图片管理接口")
public class PictureController {

    @Resource
    private PictureApplicationService pictureApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    ///  临时公共接口

    @GetMapping("/tag_category")
    @ApiOperation("分类标签菜单")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        /// 临时，固定的标签与分类
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setCategoryList(categoryList);
        pictureTagCategory.setTagList(tagList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 图片上传 MultipartFile
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件图片上传")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureUploadRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(multipartFile), ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 图片上传 URL
     *
     * @param pictureUploadRequest
     * @return
     */
    @PostMapping("/upload/url")
    @ApiOperation("URL图片上传")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(pictureUploadRequest.getFileUrl(), pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /// 管理员接口

    @PostMapping("/upload/batch")
    @ApiOperation("批量抓取图片")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureUploadByBatchRequest), ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        Integer result = pictureApplicationService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员审核图片")
    public BaseResponse<Boolean> doReviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureReviewRequest), ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.pictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 图片删除 ps:仅本人与管理员可删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation("图片删除")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 解析参数
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.deletePicture(deleteRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片
     *
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员图片更新")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(
                ObjUtil.isEmpty(pictureUpdateRequest) || pictureUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        // 管理员 转 PO 更新
        Picture picture = PictureAssembler.toPictureEntity(pictureUpdateRequest);
        // PO 标签字段是 JSONStr
        String tags = JSONUtil.toJsonStr(pictureUpdateRequest.getTags());
        picture.setTags(tags);
        User loginUser = userApplicationService.getLoginUser(request);
        // 更新
        pictureApplicationService.updatePicture(picture, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员获取图片")
    public BaseResponse<Picture> getPictureById(long id) {
        /// 校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取图片
        Picture picture = pictureApplicationService.getPictureById(id);
        return ResultUtils.success(picture);
    }

    /**
     * 管理员图片分页查询
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员图片分页查询")
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        Page<Picture> picturePage = pictureApplicationService.getListPictureByPage(pictureQueryRequest);
        return ResultUtils.success(picturePage);
    }

/// 用户接口

    /**
     * 用户图片分页查询
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo/cache")
    @ApiOperation("用户图片分页查询缓存")
    @Deprecated
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(
            @RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        Page<PictureVO> pictureVOPage = pictureApplicationService.getListPictureVOByPageWithCache(pictureQueryRequest, request);
        // 返回数据
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 用户图片分页查询
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @ApiOperation("用户图片分页查询")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(
            @RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        Page<PictureVO> pictureVOPage = pictureApplicationService.getListPictureVOByPage(pictureQueryRequest, request);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 用户获取图片
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    @ApiOperation("用户获取图片")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        PictureVO pictureVO = pictureApplicationService.getPictureVOById(id, request);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 用户图片编辑
     *
     * @param pictureEditRequest
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation("用户图片编辑")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(
            @RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(
                ObjUtil.isEmpty(pictureEditRequest) || pictureEditRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        Picture picture = PictureAssembler.toPictureEntity(pictureEditRequest);
        pictureApplicationService.editPicture(picture, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 查询颜色相似图片
     *
     * @param searchPictureByColorRequest
     * @return
     */
    @PostMapping("/search/color")
    @ApiOperation("查询颜色相似图片")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(
            @RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(searchPictureByColorRequest), ErrorCode.PARAMS_ERROR, "参数错误");
        // 获取参数
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String pictureColor = searchPictureByColorRequest.getPicColor();
        User loginUser = userApplicationService.getLoginUser(request);
        List<PictureVO> result = pictureApplicationService.searchPictureByColor(spaceId, pictureColor, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 用户批量更新图片标签和分类
     *
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @ApiOperation("用户批量更新图片标签和分类")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(
            @RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureEditByBatchRequest), ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 以图识图
     *
     * @param searchPictureByPictureRequest
     * @param request
     * @return
     */
    @PostMapping("/search/picture")
    @ApiOperation("以图识图（百度识图）")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(
            @RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(ObjUtil.isEmpty(searchPictureByPictureRequest), ErrorCode.PARAMS_ERROR, "参数错误");
        List<ImageSearchResult> imageSearchResults = pictureApplicationService.searchPictureByPicture(searchPictureByPictureRequest);
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 请求AI扩图
     *
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    @ApiOperation("请求AI扩图")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(ObjUtil.isEmpty(createPictureOutPaintingTaskRequest), ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userApplicationService.getLoginUser(request);
        // 调用创建扩图任务的服务
        CreateOutPaintingTaskResponse response = pictureApplicationService.createPictureOutPaintingTask
                (createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 获取AI扩图任务
     *
     * @param taskId
     * @return
     */
    @GetMapping("/out_painting/get_task")
    @ApiOperation("请求AI扩图")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        // 校验参数
        ThrowUtils.throwIf(ObjUtil.isEmpty(taskId), ErrorCode.PARAMS_ERROR, "参数错误");
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }
}



























