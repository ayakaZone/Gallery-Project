package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.api.aliyunai.AliYunAiApi;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.manager.upload.FilePictureUpload;
import com.yupi.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.UserService;
import com.yupi.yupicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum.PASS;
import static com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum.REVIEWING;

/**
 * @author Ayaki
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-11-23 10:59:13
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Autowired
    private UserService userService;
    @Resource
    private CosManager cosManager;
    @Autowired
    private SpaceService spaceService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private AliYunAiApi aliYunAiApi;
    /**
     * 文件上传
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param user
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User user) {
        /// 校验
        ThrowUtils.throwIf(user.getUserRole() == null, ErrorCode.NO_AUTH_ERROR);
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (ObjUtil.isNotEmpty(spaceId)) {
            // 不为空 说明要上传私有空间
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 是否有权限
            if (!user.getId().equals(space.getUserId()) && !userService.isAdmin(user)) {
                ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NO_AUTH_ERROR, "无权限上传图片到此空间");
            }
            // 校验空间数量是否超额
            if (space.getTotalCount() >= space.getMaxCount()) {
                ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.OPERATION_ERROR, "空间容纳项目数量已达上限");
            }
            // 校验空间容量是否超额
            if (space.getTotalSize() >= space.getMaxSize()) {
                ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.OPERATION_ERROR, "空间容量已达上限");
            }
        }
        // 判断是更新还是新增
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 图片有id再判断有没有数据
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.PARAMS_ERROR);
            /// 仅本人与管理员可以编辑或更新图片
            if (!oldPicture.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            /// 指定编辑的图片空间是否是原图片所在的空间
            if (spaceId == null) {
                // 如果用户未指定空间，默认指向旧图片空间
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 校验用户指定更新图片空间是否为原图片空间
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        /// 上传图片
        // 上传路径前缀
        String uploadPathPrefix;
        if (ObjUtil.isEmpty(spaceId)) {
            // 公共上传路径
            uploadPathPrefix = String.format("public/%s", user.getId());
        } else {
            // 用户空间上传路径
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        /// 区分 Url 与 MultipartFile 上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        // 上传图片并返回信息
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构建图片信息类
        Picture picture = BeanUtil.copyProperties(uploadPictureResult, Picture.class);
        // 坑!!! 表picture 的字段为 name, 表 UploadPictureResult 的字段为 picName 需要手动映射
        String picName = uploadPictureResult.getPicName();
        // 是否指定了图片的前缀
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setUserId(user.getId());
        picture.setSpaceId(spaceId);
        picture.setPicColor(uploadPictureResult.getPicColor());
        /// 补充审核信息
        fillReviewParams(picture, user);
        // 补充更新图片字段
        if (pictureId != null) {
            // 更新需要添加字段
            picture.setId(pictureId); // 图片id
            picture.setEditTime(new Date()); // 修改时间
        }
        /// 插入数据库(更新/新增)——事务
        Long finalSpaceId = spaceId;
        Picture execute = transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            // 如果是私有空间的图片
            if (ObjUtil.isNotEmpty(finalSpaceId)) {
                // 更新空间容量和项目数量-自定义SQL
                boolean updateResult = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId).setSql("totalSize = totalSize + " + picture.getPicSize()).setSql("totalCount = totalCount + 1").update();
                ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库更新失败");
            }
            // 封装VO对象
            return picture;
        });
        return PictureVO.objToVo(execute);
    }

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
     * 获得 PictureVO
     *
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        ///  VO 中包含创建图片的用户信息
        // 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.NOT_FOUND_ERROR);
        // 转PictureVO
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 获取用户id
        Long userId = picture.getUserId();
        // 校验
        if (userId != null && userId > 0) {
            // 获取用户信息-Session方式
            // Object objUser = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
            // User user = (User) objUser;
            // 获取用户信息-getById()
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * Picture分页PO转VO
     *
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 获得page
        List<Picture> records = picturePage.getRecords();
        // 封装 VO 的分页参数
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        // 校验
        if (CollUtil.isEmpty(records)) {
            return pictureVOPage;
        }
        // 封装 VO
        List<PictureVO> pictureVOList = records.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        ///  一个用户可以有多张图片，使用 Map<UserId,List<PictureVO>
        // 获取 userId 集合
        Set<Long> userIdSet = records.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 可以使用 userId 匹配 user 的 map 集合
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 封装 pictureVOList 的 user信息
        pictureVOList.forEach(pictureVO -> {
            // 获得图片关联的 userId
            Long userId = pictureVO.getUserId();
            // 查 user，1个id对应一个user所以是get(0)
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            // 把 userVO 对象封装进 PictureVO 中
            pictureVO.setUser(userService.getUserVO(user));
        });
        // 返回
        return pictureVOPage.setRecords(pictureVOList);
    }

    /**
     * 图片校验
     *
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ///  校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
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
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        // 判断是否重审
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");
        // 操作数据库更新审核状态
        Picture updatePicture = BeanUtil.copyProperties(pictureReviewRequest, Picture.class);
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewerId(loginUser.getId());
        boolean result = this.updateById(updatePicture);
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
        if (userService.isAdmin(loginUser)) {
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
     * 批量抓取图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 获得参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        // 一次最多抓取 30 条
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "单次最多抓取 30 条");
        // 抓取地址
        String fetchUlr = String.format("https://www.bing.com/images/async?q=%s&mmasync=1", searchText);
        // 获取 html 文档
        Document document;
        try {
            document = Jsoup.connect(fetchUlr).get();
        } catch (Exception e) {
            log.info("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析文档，获得Class元素 <div>
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        // 从 div 中获取抓取的所有图片
        Elements imgElementList = div.select("img.mimg");
        // 获得图片的 src 并上传
        int uploadSuccessCount = 0;
        for (Element imgElement : imgElementList) {
            // 获得 url
            String src = imgElement.attr("src");
            if (StrUtil.isBlank(src)) {
                log.info("当前图片url为空,已跳过 {}", src);
                continue;
            }
            // 处理成干净的 url
            int questionIndex = src.indexOf("?");
            if (questionIndex > -1) {
                src = src.substring(0, questionIndex);
            }
            // 获取指定的图片前缀，没有传入默认为搜索关键词
            String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
            if (StrUtil.isBlank(namePrefix)) {
                namePrefix = searchText;
            }
            // 设置图片前缀
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setPicName(namePrefix + "_" + (uploadSuccessCount + 1));
            // 上传图片
            try {
                PictureVO pictureVO = uploadPicture(src, pictureUploadRequest, loginUser);
                log.info("图片上传成功 {}", pictureVO.getId());
                uploadSuccessCount++;
            } catch (Exception e) {
                log.info("图片上传失败", e);
                continue;
            }
            if (uploadSuccessCount >= count) {
                break;
            }
        }
        return uploadSuccessCount;
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
        long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
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
     * 图片校验权限操作
     *
     * @param user
     * @param picture
     */
    @Override
    public void checkPictureAuth(User user, Picture picture) {
        Long spaceId = picture.getSpaceId();
        // 公共图库
        if (ObjUtil.isEmpty(spaceId)) {
            // 仅本人和管理员可以对图片操作
            if (!picture.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此图片");
            }
        } else { // 私有空间
            // 仅本人对图片操作
            if (!picture.getUserId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此图片");
            }
        }
    }

    /**
     * 用户编辑图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        /// 编辑
        // 用户 转 Picture 更新
        Picture picture = BeanUtil.copyProperties(pictureEditRequest, Picture.class);
        // PO 标签字段是 JSONStr
        String tags = JSONUtil.toJsonStr(pictureEditRequest.getTags());
        picture.setTags(tags);
        /// 设置用户编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 数据库是否存在待更新的图片
        Long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        /// 仅用户本人和管理员可编辑
        // 权限校验
        this.checkPictureAuth(loginUser, oldPicture);
        // 补充审核信息
        this.fillReviewParams(picture, loginUser);
        // 更新图片
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 用户管理员删除图片
     *
     * @param deleteRequest
     * @param loginUser
     */
    public void deletePicture(DeleteRequest deleteRequest, User loginUser) {
        Long id = deleteRequest.getId();
        // 图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        // 权限校验
        this.checkPictureAuth(loginUser, oldPicture);
        // 逻辑删除
        transactionTemplate.execute(status -> {
            // 删除数据
            boolean removeResult = this.removeById(id);
            ThrowUtils.throwIf(!removeResult, ErrorCode.OPERATION_ERROR);
            // 如果删除的是私人空间的图片
            Long spaceId = oldPicture.getSpaceId();
            if (ObjUtil.isNotEmpty(spaceId)) {
                // 更新空间容量和项目数量-自定义SQL
                boolean updateResult = spaceService.lambdaUpdate().eq(Space::getId, spaceId).setSql("totalSize = totalSize - " + oldPicture.getPicSize()).setSql("totalCount = totalCount - 1").update();
                ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "图片删除失败，数据库更新失败");
            }
            return true;
        });
        // 删除COS对象存储中的缩略图
        this.clearPictureFile(oldPicture);
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
    public List<PictureVO> searchPictureByColor(Long SpaceId, String picColor, User login) {
        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR, "请输入图片颜色");
        ThrowUtils.throwIf(ObjUtil.isEmpty(SpaceId), ErrorCode.PARAMS_ERROR, "请指定查询图片颜色相似度的空间");
        ThrowUtils.throwIf(ObjUtil.isEmpty(login), ErrorCode.NO_AUTH_ERROR, "用户未登录");
        // 校验空间
        Space space = spaceService.getById(SpaceId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 校验权限
        if (!space.getUserId().equals(login.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此空间");
        }
        // 16进制图片颜色转为Color对象
        Color color = Color.decode(picColor);
        // 从数据库查询指定空间所有图片
        List<Picture> pictureList = lambdaQuery().eq(Picture::getSpaceId, SpaceId).isNotNull(Picture::getPicColor).list();
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
                .limit(12)
                .collect(Collectors.toList());
        // 转 VO 返回
        return pictures.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

    /**
     * 用户批量编辑图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 获取参数
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList) || ObjUtil.isEmpty(spaceId),
                ErrorCode.PARAMS_ERROR, "请指定要编辑的图片");
        ThrowUtils.throwIf(ObjUtil.isEmpty(loginUser), ErrorCode.PARAMS_ERROR, "登录用户不存在");
        // 校验空间
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 权限校验
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此空间");
        }
        // 查询所有需要编辑的图片
        List<Picture> pictureList = lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        // 校验
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureList), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 用户命名规则处理
        fillPictureWithNameRule(pictureList, nameRule);
        // 批量编辑图片
        pictureList.forEach(picture -> {
            // 分类
            if (StrUtil.isNotBlank( category)) {
                picture.setCategory(category);
            }
            // 标签
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 更新数据库
        boolean result = updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片批量编辑失败");
    }

    /**
     * 用户命名规则处理
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
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
        Picture picture = getById(pictureId);
        Optional.ofNullable(picture)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 校验权限
        ThrowUtils.throwIf(!picture.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权操作此图片");
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
}
