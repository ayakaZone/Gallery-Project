package yupicture.domain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yupicturebackend.manager.auth.StpKit;
import com.yupi.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupicturebackend.model.entity.Space;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import yupicture.domain.picture.entity.Picture;
import yupicture.domain.picture.repository.PictureRepository;
import yupicture.domain.service.PictureDomainService;
import yupicture.domain.user.entity.User;
import yupicture.infrastructure.api.CosManager;
import yupicture.infrastructure.api.aliyunai.AliYunAiApi;
import yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import yupicture.infrastructure.common.ResultUtils;
import yupicture.infrastructure.exception.BusinessException;
import yupicture.infrastructure.exception.ErrorCode;
import yupicture.infrastructure.exception.ThrowUtils;
import yupicture.infrastructure.utils.ColorSimilarUtils;
import yupicture.interfaces.dto.picture.*;
import yupicture.interfaces.vo.picture.PictureVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static yupicture.domain.picture.valueobject.PictureReviewStatusEnum.PASS;
import static yupicture.domain.picture.valueobject.PictureReviewStatusEnum.REVIEWING;

/**
 * @author Ayaki
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-11-23 10:59:13
 */
@Slf4j
@Service
public class PictureDomainServiceImpl implements PictureDomainService {

    @Resource
    private CosManager cosManager;
    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private PictureRepository pictureRepository;

    /**
     * 封装查询条件
     *
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        // 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryRequest), ErrorCode.PARAMS_ERROR);
        // 查询参数
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText(); // 多字段查询
        Long userId = pictureQueryRequest.getUserId();
        String sortOrder = pictureQueryRequest.getSortOrder();
        String sortField = pictureQueryRequest.getSortField();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        /// 查询条件
        QueryWrapper<Picture> QueryWrapper = new QueryWrapper<>();
        // 多字段查询需要拼接条件
        if (StrUtil.isNotBlank(searchText)) {
            QueryWrapper.and(qw -> {
                qw.like("name", searchText).or().like("introduction", searchText);
            });
        }
        // 普通查询条件
        QueryWrapper<Picture> queryWrapper = QueryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id).eq(ObjUtil.isNotEmpty(userId), "userId", userId).eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId).like(StrUtil.isNotBlank(name), "name", name).like(StrUtil.isNotBlank(introduction), "introduction", introduction).like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat).eq(StrUtil.isNotBlank(category), "category", category).eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize).eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth).eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight).eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale).eq(ObjUtil.isNotEmpty(reviewMessage), "reviewMessage", reviewMessage).eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus).eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId).isNull(nullSpaceId, "spaceId").ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime).lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // 多标签查询条件
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序条件
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        // 返回
        return queryWrapper;
    }

    /**
     * 图片校验
     *
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        picture.validPicture();
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void pictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 获取所有参数
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        // 校验参数
        if (ObjUtil.isEmpty(id) || ObjUtil.isEmpty(reviewMessage) || StrUtil.isBlank(reviewMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 需要审核的图片是否存在
        Picture oldPicture = pictureRepository.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        // 判断是否重审
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");
        // 操作数据库更新审核状态
        Picture updatePicture = BeanUtil.copyProperties(pictureReviewRequest, Picture.class);
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewerId(loginUser.getId());
        boolean result = pictureRepository.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 审核字段填充
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 管理员操作自动过审
        if (loginUser.isAdmin()) {
            picture.setReviewStatus(PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        } else {
            // 给管理员操作需要更改为待审核
            picture.setReviewStatus(REVIEWING.getValue());
        }
    }

    /**
     * 异步清理COS对象存储文件
     *
     * @param oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = pictureRepository.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        String COS_POST = "https://ayaka-picture-1387860168.cos.ap-shanghai.myqcloud.com/";
        String url = StrUtil.removePrefix(oldPicture.getUrl(), COS_POST);
        cosManager.deleteObject(url);
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    /**
     * 用户编辑图片
     *
     * @param picture
     * @param loginUser
     */
    public void editPicture(Picture picture, User loginUser) {
        /// 编辑
        // PO 标签字段是 JSONStr
        String tags = JSONUtil.toJsonStr(picture.getTags());
        picture.setTags(tags);
        /// 设置用户编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 数据库是否存在待更新的图片
        Long id = picture.getId();
        Picture oldPicture = pictureRepository.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        /// 仅用户本人和管理员可编辑
        // 权限校验——已使用Sa-Token鉴权
        // pictureRepository.checkPictureAuth(loginUser, oldPicture);
        // 补充审核信息
        this.fillReviewParams(picture, loginUser);
        // 更新图片
        boolean result = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 根据图片颜色相似度查找图片列表
     *
     * @param SpaceId
     * @param picColor
     * @param login
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long SpaceId, String picColor, User login, Space space) {
        // 校验权限
        if (!space.getUserId().equals(login.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此空间");
        }
        // 16进制图片颜色转为Color对象
        Color color = Color.decode(picColor);
        // 从数据库查询指定空间所有图片
        List<Picture> pictureList = pictureRepository.lambdaQuery().eq(Picture::getSpaceId, SpaceId).isNotNull(Picture::getPicColor).list();
        // 校验是否有指定图片
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        // 遍历图片集合
        List<Picture> pictures = pictureList.stream()
                // 计算颜色相似度并排序
                .sorted(Comparator.comparingDouble(picture -> {
                    String pictureColor = picture.getPicColor();
                    // 没有颜色参数就默认最大排序在最后面
                    if (StrUtil.isBlank(pictureColor)) {
                        return Double.MAX_VALUE;
                    }
                    // 16进制转 Color
                    Color decode = Color.decode(pictureColor);
                    // 计算图片颜色相似度
                    return -ColorSimilarUtils.getSimilarity(color, decode);
                }))
                // 只取前12个
                .limit(12).collect(Collectors.toList());
        // 转 VO 返回
        return pictures.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

    /**
     * 用户命名规则处理
     *
     * @param pictureList
     * @param nameRule
     */
    public void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        // 用户命名规则为空就不处理
        if (StrUtil.isBlank(nameRule)) {
            return;
        }
        try {
            AtomicLong count = new AtomicLong(1);
            pictureList.forEach(picture -> {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count.getAndIncrement()));
                picture.setName(pictureName);
            });
        } catch (Exception e) {
            log.error("用户命名规则解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

    /**
     * 创建图片扩展任务
     *
     * @param request
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest request, User loginUser) {
        // 校验参数
        Long pictureId = request.getPictureId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureId), ErrorCode.PARAMS_ERROR, "请指定要处理的图片");
        Picture picture = getPictureById(pictureId);
        Optional.ofNullable(picture).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 封装请求参数的 url字段
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);
        // 拷贝其余参数
        BeanUtil.copyProperties(request, createOutPaintingTaskRequest);
        // 调用Api发起请求
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }

    /**
     * 批量查询图片
     *
     * @param spaceId
     * @param pictureIdList
     * @return
     */
    @Override
    public List<Picture> pictureLambdaQuery(Long spaceId, List<Long> pictureIdList) {
        List<Picture> list = pictureRepository.lambdaQuery().select(Picture::getId, Picture::getSpaceId).eq(Picture::getSpaceId, spaceId).in(Picture::getId, pictureIdList).list();
        ThrowUtils.throwIf(CollUtil.isEmpty(list), ErrorCode.NOT_FOUND_ERROR);
        return list;
    }

    /**
     * 管理员更新图片
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void updatePicture(Picture picture, User loginUser) {
        /// 更新
        // 数据校验
        this.validPicture(picture);
        // 数据库是否存在待更新的图片
        Long id = loginUser.getId();
        Picture oldPicture = pictureRepository.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 更新图片
        boolean result = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 获取图片列表
     *
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public Page<Picture> getListPictureByPage(PictureQueryRequest pictureQueryRequest) {
        // 页面条件
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 分页查询
        return pictureRepository.page(new Page<>(current, pageSize), this.getQueryWrapper(pictureQueryRequest));
    }

    /**
     * 获取图片列表VO(上层转VO)
     *
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Override
    public Page<Picture> getListPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 页面条件
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        /// 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.OPERATION_ERROR);
        // 判断查询公共图库还是私有空间
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (ObjUtil.isEmpty(spaceId)) { // 公共图库
            // 设置查询公共图库的字段
            pictureQueryRequest.setNullSpaceId(true);
            // 设置用户只能查询已过审的图片
            pictureQueryRequest.setReviewStatus(PASS.getValue());
        } else { // 私有空间
            // 使用Sa-Token权限校验
            boolean result = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!result, ErrorCode.NO_AUTH_ERROR, "无权查看图片");
            /*Space space = spaceService.getById(spaceId);
            User loginUser = userApplicationService.getLoginUser(request);
            // 空间校验
                ThrowUtils.throwIf(ObjUtil.isEmpty(space),
                    ErrorCode.NOT_FOUND_ERROR, "查询图片指定的空间不存在");
            // 权限校验
            if (!loginUser.getId().equals(space.getUserId()) && !userApplicationService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有查看该图片所在空间的权限");
            }*/
        }
        // 分页查询
        return pictureRepository.page(new Page<>(current, pageSize), this.getQueryWrapper(pictureQueryRequest));
    }

    /**
     * 获取分页数据
     * @param objectPage
     * @param queryWrapper
     * @return
     */
    @Override
    public Page<Picture> getPage(Page<Picture> objectPage, QueryWrapper<Picture> queryWrapper) {
        return pictureRepository.page(objectPage, queryWrapper);
    }

    /**
     * 根据图片ID获取图片
     *
     * @param pictureId
     * @return
     */
    @Override
    public Picture getPictureById(Long pictureId) {
        return pictureRepository.getById(pictureId);
    }

    /**
     * 保存图片
     *
     * @param picture
     * @return
     */
    @Override
    public boolean pictureSaveOrUpdate(Picture picture) {
        return pictureRepository.saveOrUpdate(picture);
    }

    /**
     * 删除图片
     *
     * @param id
     * @return
     */
    @Override
    public boolean removePictureById(Long id) {
        return pictureRepository.removeById(id);
    }

    /**
     * 批量更新图片
     *
     * @param pictureList
     * @return
     */
    @Override
    public boolean updatePictureBatchById(List<Picture> pictureList) {
        return pictureRepository.updateBatchById(pictureList);
    }
}
