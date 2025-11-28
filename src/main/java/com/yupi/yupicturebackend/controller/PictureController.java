package com.yupi.yupicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.model.vo.PictureTagCategory;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.spring.web.json.Json;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum.PASS;

@RestController
@RequestMapping("/picture")
@Slf4j
@Api(tags = "图片管理接口")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /// 构造本地缓存
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

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
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
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
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(pictureUploadRequest.getFileUrl(), pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /// 管理员接口

    @PostMapping("/upload/batch")
    @ApiOperation("批量抓取图片")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureUploadByBatchRequest), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer result = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 图片校验
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
        User loginUser = userService.getLoginUser(request);
        pictureService.pictureReview(pictureReviewRequest, loginUser);
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
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 解析参数
        Long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        // 图片是否存在
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        // 仅本人与管理员可删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 逻辑删除
        boolean removeResult = pictureService.removeById(id);
        ThrowUtils.throwIf(!removeResult, ErrorCode.OPERATION_ERROR);
        // 删除COS对象存储中的缩略图
        pictureService.clearPictureFile(oldPicture);
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
    public BaseResponse<Boolean> updatePicture(
            @RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(
                ObjUtil.isEmpty(pictureUpdateRequest) || pictureUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        /// 更新
        // 管理员 转 PO 更新
        Picture picture = BeanUtil.copyProperties(pictureUpdateRequest, Picture.class);
        // PO 标签字段是 JSONStr
        String tags = JSONUtil.toJsonStr(pictureUpdateRequest.getTags());
        picture.setTags(tags);
        // 数据校验
        pictureService.validPicture(picture);
        // 数据库是否存在待更新的图片
        Long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        pictureService.fillReviewParams(picture, userService.getLoginUser(request));
        // 更新图片
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员获取图片")
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取图片
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.NOT_FOUND_ERROR);
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
        // 页面条件
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 分页查询
        Page<Picture> picturePage = pictureService.page(
                new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
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
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(
            @RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 页面条件
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        /// 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.OPERATION_ERROR);
        // 设置用户只能查询已过审的图片
        pictureQueryRequest.setReviewStatus(PASS.getValue());
        /// Redis 中是否存在缓存
        // 缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = "yupicture:listPictureVOByPageWithCache:" + hashKey;
        // 查询是否命中本地缓存 Caffeine
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(cacheValue)) {
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(pictureVOPage);
        }
        // 查询是否命中 Redis 缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String redisValue = opsForValue.get(cacheKey);
        if (StrUtil.isNotBlank(redisValue)) {
            // 同步本地缓存 Caffeine
            LOCAL_CACHE.put(cacheKey, redisValue);
            // 命中缓存，直接返回
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(redisValue, Page.class);
            return ResultUtils.success(pictureVOPage);
        }
        // 未命中缓存，分页查询数据库
        Page<Picture> picturePage = pictureService.page(
                new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 图片分页转 VO
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        // 把数据转成 JsonStr
        String cachedValue = JSONUtil.toJsonStr(pictureVOPage); // 值
        // 把数据缓存到 Caffeine
        LOCAL_CACHE.put(cacheKey, cachedValue);
        // 把数据缓存到 Redis
        int TTL = 300 + RandomUtil.randomInt(0, 300); // 过期时间 秒
        opsForValue.set(cacheKey, cachedValue, TTL, TimeUnit.SECONDS);
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
        // 页面条件
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        /// 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.OPERATION_ERROR);
        // 设置用户只能查询已过审的图片
        pictureQueryRequest.setReviewStatus(PASS.getValue());
        // 分页查询
        Page<Picture> picturePage = pictureService.page(
                new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 图片分页转 VO
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
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
        // 获取图片
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.NOT_FOUND_ERROR);
        // 校验图片的审核状态
        ThrowUtils.throwIf(!picture.getReviewStatus().equals(PASS.getValue()), ErrorCode.PARAMS_ERROR, "仅能获取已过审图片");
        // 转VO
        PictureVO pictureVO = PictureVO.objToVo(picture);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 用户图片编辑
     *
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation("用户图片编辑")
    public BaseResponse<Boolean> editPicture(
            @RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(
                ObjUtil.isEmpty(pictureUpdateRequest) || pictureUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        /// 编辑
        // 用户 转 Picture 更新
        Picture picture = BeanUtil.copyProperties(pictureUpdateRequest, Picture.class);
        // PO 标签字段是 JSONStr
        String tags = JSONUtil.toJsonStr(pictureUpdateRequest.getTags());
        picture.setTags(tags);
        /// 设置用户编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        pictureService.validPicture(picture);
        // 数据库是否存在待更新的图片
        Long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        /// 仅用户本人和管理员可编辑
        User loginUser = userService.getLoginUser(request);
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 补充审核信息
        pictureService.fillReviewParams(picture, loginUser);
        // 更新图片
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


}



























