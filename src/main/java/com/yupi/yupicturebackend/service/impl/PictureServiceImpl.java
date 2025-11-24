package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.FileManager;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ayaki
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-11-23 10:59:13
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;
    @Autowired
    private UserService userService;

    /**
     * 文件上传
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param user
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User user) {
        /// 校验
        ThrowUtils.throwIf(user.getUserRole() == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是更新还是新增
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 图片有id再判断有没有数据
        if (pictureId != null) {
            boolean exists = lambdaQuery().eq(Picture::getId, pictureId).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        /// 上传图片
        // 公共上传路径
        String uploadPathPrefix = String.format("public/%s", user.getId());
        // 上传图片并返回信息
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // 构建图片信息类
        Picture picture = BeanUtil.copyProperties(uploadPictureResult, Picture.class);
        // 坑!!! 表picture 的字段为 name, 表 UploadPictureResult 的字段为 picName 需要手动映射
        picture.setName(uploadPictureResult.getPicName());
        picture.setUserId(user.getId());
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
        /// 查询条件
        QueryWrapper<Picture> QueryWrapper = new QueryWrapper<>();
        // 多字段查询需要拼接条件
        if (StrUtil.isNotBlank(searchText)) {
            QueryWrapper.and(qw -> {
                qw.like("name", searchText).or().like("introduction", searchText);
            });
        }
        // 普通查询条件
        QueryWrapper<Picture> queryWrapper = QueryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id).eq(ObjUtil.isNotEmpty(userId), "userId", pictureQueryRequest.getUserId()).like(StrUtil.isNotBlank(name), "name", name).like(StrUtil.isNotBlank(introduction), "introduction", introduction).like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat).eq(StrUtil.isNotBlank(category), "category", category).eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize).eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth).eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight).eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
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
}
