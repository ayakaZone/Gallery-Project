package yupicture.application.service.impl;

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
import com.yupi.yupicturebackend.manager.auth.SpaceUserAuthManager;
import com.yupi.yupicturebackend.manager.auth.StpKit;
import com.yupi.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupicturebackend.manager.upload.FilePictureUpload;
import com.yupi.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import yupicture.application.service.PictureApplicationService;
import yupicture.application.service.UserApplicationService;
import yupicture.domain.picture.entity.Picture;
import yupicture.domain.service.PictureDomainService;
import yupicture.domain.user.entity.User;
import yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import yupicture.infrastructure.api.imagesearch.ImageSearchApiFacade;
import yupicture.infrastructure.api.imagesearch.model.ImageSearchResult;
import yupicture.infrastructure.common.DeleteRequest;
import yupicture.infrastructure.exception.BusinessException;
import yupicture.infrastructure.exception.ErrorCode;
import yupicture.infrastructure.exception.ThrowUtils;
import yupicture.interfaces.dto.picture.*;
import yupicture.interfaces.vo.picture.PictureVO;
import yupicture.interfaces.vo.user.UserVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static yupicture.domain.picture.valueobject.PictureReviewStatusEnum.PASS;

/**
 * @author Ayaki
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-11-23 10:59:13
 */
@Slf4j
@Service
public class PictureApplicationServiceImpl implements PictureApplicationService {

    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private PictureDomainService pictureDomainService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /// 构造本地缓存
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

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
            if (!user.getId().equals(space.getUserId()) && !user.isAdmin()) {
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
            Picture oldPicture = pictureDomainService.getPictureById(pictureId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.PARAMS_ERROR);
            /// 仅本人与管理员可以编辑或更新图片
            if (!oldPicture.getUserId().equals(user.getId()) && !user.isAdmin()) {
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
            boolean result = pictureDomainService.pictureSaveOrUpdate(picture);
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
        QueryWrapper<Picture> queryWrapper = pictureDomainService.getQueryWrapper(pictureQueryRequest);
        ThrowUtils.throwIf(ObjUtil.isEmpty(queryWrapper), new BusinessException(ErrorCode.OPERATION_ERROR, "查询条件为空"));
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
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
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
        List<PictureVO> pictureVOList = records.stream().map(picture -> {
            return getPictureVO(picture, request);
        }).collect(Collectors.toList());
        ///  一个用户可以有多张图片，使用 Map<UserId,List<PictureVO>
        // 获取 userId 集合
        Set<Long> userIdSet = records.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 可以使用 userId 匹配 user 的 map 集合
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
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
            pictureVO.setUser(userApplicationService.getUserVO(user));
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
        pictureDomainService.validPicture(picture);
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void pictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 图片审核
        pictureDomainService.pictureReview(pictureReviewRequest, loginUser);
    }

    /**
     * 审核字段填充
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 图片审核字段填充
        pictureDomainService.fillReviewParams(picture, loginUser);
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
     * 用户编辑图片
     *
     * @param picture
     * @param loginUser
     */
    public void editPicture(Picture picture, User loginUser) {
        // 用户编辑图片
        pictureDomainService.editPicture(picture, loginUser);
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
        Picture oldPicture = pictureDomainService.getPictureById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        // 权限校验——已使用Sa-Token鉴权
        // pictureDomainService.checkPictureAuth(loginUser, oldPicture);
        // 逻辑删除
        transactionTemplate.execute(status -> {
            // 删除数据
            boolean removeResult = pictureDomainService.removePictureById(id);
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
        pictureDomainService.clearPictureFile(oldPicture);
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
        // 查询图片列表
        ThrowUtils.throwIf(StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR, "请输入图片颜色");
        ThrowUtils.throwIf(ObjUtil.isEmpty(SpaceId), ErrorCode.PARAMS_ERROR, "请指定查询图片颜色相似度的空间");
        ThrowUtils.throwIf(ObjUtil.isEmpty(login), ErrorCode.NO_AUTH_ERROR, "用户未登录");
        // 校验空间
        Space space = spaceService.getById(SpaceId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        List<PictureVO> pictureVOS = pictureDomainService.searchPictureByColor(SpaceId, picColor, login, space);
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureVOS), ErrorCode.NOT_FOUND_ERROR, "未找到图片");
        return pictureVOS;
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
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList) || ObjUtil.isEmpty(spaceId), ErrorCode.PARAMS_ERROR, "请指定要编辑的图片");
        ThrowUtils.throwIf(ObjUtil.isEmpty(loginUser), ErrorCode.PARAMS_ERROR, "登录用户不存在");
        // 校验空间
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 权限校验
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此空间");
        }
        // 查询所有需要编辑的图片
        List<Picture> pictureList = pictureDomainService.pictureLambdaQuery(spaceId, pictureIdList);
        // 校验
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureList), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 用户命名规则处理
        pictureDomainService.fillPictureWithNameRule(pictureList, nameRule);
        // 批量编辑图片
        pictureList.forEach(picture -> {
            // 分类
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            // 标签
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 更新数据库
        boolean result = pictureDomainService.updatePictureBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片批量编辑失败");
    }

    /**
     * 创建图片扩展任务
     *
     * @param request
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest request, User loginUser) {
        // 创建图片扩展任务
        CreateOutPaintingTaskResponse pictureOutPaintingTask = pictureDomainService.createPictureOutPaintingTask(request, loginUser);
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureOutPaintingTask), ErrorCode.OPERATION_ERROR, "创建扩图任务失败");
        return pictureOutPaintingTask;
    }

    /**
     * 管理员更新图片
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void updatePicture(Picture picture, User loginUser) {
        // 管理员更新图片
        pictureDomainService.updatePicture(picture, loginUser);
    }

    /**
     * 获取图片
     *
     * @param id
     * @return
     */
    @Override
    public Picture getPictureById(long id) {
        Picture picture = pictureDomainService.getPictureById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return picture;
    }

    /**
     * 获取图片列表
     *
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public Page<Picture> getListPictureByPage(PictureQueryRequest pictureQueryRequest) {
        Page<Picture> picturePage = pictureDomainService.getListPictureByPage(pictureQueryRequest);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picturePage), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return picturePage;
    }

    /**
     * 获取图片VO分页
     *
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getListPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        Page<Picture> picturePage = pictureDomainService.getListPictureVOByPage(pictureQueryRequest, request);
        // 图片分页转 VO
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureVOPage), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return pictureVOPage;
    }

    /**
     * 获取图片VO
     *
     * @param id
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVOById(long id, HttpServletRequest request) {
        // 获取图片VO

        // 获取图片
        Picture picture = pictureDomainService.getPictureById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.NOT_FOUND_ERROR);
        Space space = null;
        // 校验权限
        if (ObjUtil.isNotEmpty(picture.getSpaceId())) { // 私有空间图片
            // 校验空间——使用Sa-Token编程式权限校验
            boolean result = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!result, ErrorCode.NO_AUTH_ERROR, "无权查看图片");
            space = spaceService.getById(picture.getSpaceId());
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "查询图片指定的空间不存在");
            /*Space space = spaceService.getById(picture.getSpaceId());
            ThrowUtils.throwIf(ObjUtil.isEmpty(space),
                    ErrorCode.NOT_FOUND_ERROR, "查询图片指定的空间不存在");*/
            // 校验权限
            /*if (!picture.getUserId().equals(userApplicationService.getLoginUser(request).getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权获得此图片");
            }*/
        } else { // 公共图库
            // 校验图片的审核状态
            ThrowUtils.throwIf(!picture.getReviewStatus().equals(PASS.getValue()), ErrorCode.PARAMS_ERROR, "仅能获取已过审图片");
        }
        User loginUser = userApplicationService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 转VO
        PictureVO pictureVO = this.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureVO), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return pictureVO;

    }

    /**
     * 百度以图搜图
     *
     * @param searchPictureByPictureRequest
     * @return
     */
    @Override
    public List<ImageSearchResult> searchPictureByPicture(SearchPictureByPictureRequest searchPictureByPictureRequest) {
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureId) || pictureId <= 0, ErrorCode.PARAMS_ERROR, "参数错误");
        Picture picture = this.getPictureById(pictureId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 调用以图识图API
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(picture.getUrl());
        ThrowUtils.throwIf(ObjUtil.isEmpty(imageSearchResults), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return imageSearchResults;
    }

    /**
     * 获取图片VO分页
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getListPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
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
            return JSONUtil.toBean(cacheValue, Page.class);
        }
        // 查询是否命中 Redis 缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String redisValue = opsForValue.get(cacheKey);
        if (StrUtil.isNotBlank(redisValue)) {
            // 同步本地缓存 Caffeine
            LOCAL_CACHE.put(cacheKey, redisValue);
            // 命中缓存，直接返回
            return JSONUtil.toBean(redisValue, Page.class);
        }
        // 未命中缓存，分页查询数据库
        Page<Picture> picturePage = pictureDomainService
                .getPage(new Page<Picture>(current, pageSize), this.getQueryWrapper(pictureQueryRequest));
        // 图片分页转 VO
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);
        // 把数据转成 JsonStr
        String cachedValue = JSONUtil.toJsonStr(pictureVOPage); // 值
        // 把数据缓存到 Caffeine
        LOCAL_CACHE.put(cacheKey, cachedValue);
        // 把数据缓存到 Redis
        int TTL = 300 + RandomUtil.randomInt(0, 300); // 过期时间 秒
        opsForValue.set(cacheKey, cachedValue, TTL, TimeUnit.SECONDS);
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureVOPage), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return pictureVOPage;
    }
}
