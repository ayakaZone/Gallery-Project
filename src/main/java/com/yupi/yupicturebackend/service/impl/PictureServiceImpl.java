package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        }
        /// 上传图片
        // 公共上传路径
        String uploadPathPrefix = String.format("public/%s", user.getId());
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
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setUserId(user.getId());
        /// 补充审核信息
        fillReviewParams(picture, user);
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
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        /// 查询条件
        QueryWrapper<Picture> QueryWrapper = new QueryWrapper<>();
        // 多字段查询需要拼接条件
        if (StrUtil.isNotBlank(searchText)) {
            QueryWrapper.and(qw -> {
                qw.like("name", searchText).or().like("introduction", searchText);
            });
        }
        // 普通查询条件
        QueryWrapper<Picture> queryWrapper = QueryWrapper
                .eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId)
                .like(StrUtil.isNotBlank(name), "name", name)
                .like(StrUtil.isNotBlank(introduction), "introduction", introduction)
                .like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat)
                .eq(StrUtil.isNotBlank(category), "category", category)
                .eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize)
                .eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth)
                .eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight)
                .eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale)
                .eq(ObjUtil.isNotEmpty(reviewMessage), "reviewMessage", reviewMessage)
                .eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
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
     * @param oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // FIXME 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        String COS_POST = "https://ayaka-picture-1387860168.cos.ap-shanghai.myqcloud.com/";
        String url = StrUtil.removePrefix(oldPicture.getUrl(), COS_POST);
        cosManager.deleteObject(url);
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }
}
