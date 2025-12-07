package com.yupi.yupicture.application.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicture.interfaces.dto.space.analyze.*;
import com.yupi.yupicture.interfaces.vo.space.analyze.*;
import org.springframework.stereotype.Service;
import com.yupi.yupicture.application.service.SpaceAnalyzeApplicationService;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.domain.picture.repository.PictureRepository;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author Ayaki
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-11-28 18:24:24
 */
@Service
public class SpaceAnalyzeApplicationServiceImpl implements SpaceAnalyzeApplicationService {

    @Resource
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private PictureRepository pictureRepository;

    /**
     * 查询空间使用分析
     *
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        /// 参数校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUsageAnalyzeRequest), ErrorCode.PARAMS_ERROR, "参数不能为空");
        // 获得参数
        Boolean queryAll = spaceUsageAnalyzeRequest.isQueryAll();
        Boolean queryPublic = spaceUsageAnalyzeRequest.isQueryPublic();
        // 根据查询范围填充查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (queryAll || queryPublic) {
            // 权限校验:仅管理员权限
            ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR);
            // 查询公共图库需要增加条件
            if (spaceUsageAnalyzeRequest.isQueryPublic()) {
                queryWrapper.isNull("spaceId");
            }
            // 设置查询字段
            queryWrapper.select("picSize");
            // 查询数据库
            List<Object> pictureObjList = pictureRepository.getBaseMapper().selectObjs(queryWrapper);
            // 转换数据类型并统计使用总大小
            long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Number ? ((Long) result) : 0).sum();
            // 统计使用总数量
            long usedCount = pictureObjList.size();
            // 构造响应体
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            // 返回结果
            return spaceUsageAnalyzeResponse;
        } else {
            // 仅用户和管理员本人
            // 1. 校验空间是否存在
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(ObjUtil.isEmpty(spaceId), ErrorCode.PARAMS_ERROR, "空间ID不能为空");
            Space space = spaceApplicationService.getSpaceById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 2. 校验空间权限
            spaceApplicationService.checkSpaceAuth(loginUser, space);
            // 查询数据
            Long totalSize = space.getTotalSize();
            Long totalCount = space.getTotalCount();
            Long maxSize = space.getMaxSize();
            Long maxCount = space.getMaxCount();
            // 计算使用占比
            double sizeUsageRatio = totalSize * 100.0 / maxSize;
            double countUsageRatio = totalCount * 100.0 / maxCount;
            // 构造响应体
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedCount(totalCount);
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setMaxCount(maxCount);
            spaceUsageAnalyzeResponse.setMaxSize(maxSize);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            // 返回结果
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 查询图片分类分析
     *
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        // 根据查询范围校验权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 根据查询范围填充查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        // 设置查询字段、并按标签分组统计数量和大小
        queryWrapper.select("category AS category", "COUNT(*) AS count", "SUM(picSize) AS totalSize").groupBy("category");
        // 查询数据库
        List<Map<String, Object>> maps = pictureRepository.getBaseMapper().selectMaps(queryWrapper);
        // 流处理封装为响应类
        List<SpaceCategoryAnalyzeResponse> list = maps.stream().map(result -> {
            String category = result.get("category") != null ? result.get("category").toString() : "未分类";
            long count = ((Number) result.get("count")).longValue();
            long totalSize = ((Number) result.get("totalSize")).longValue();
            return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        }).collect(Collectors.toList());
        // 返回结果
        return list;
    }

    /**
     * 查询图片标签分析
     *
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        // 根据查询范围校验权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 根据查询范围填充查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        // 设置查询字段
        queryWrapper.select("tags");
        // 查询数据库
        List<Object> objectList = pictureRepository.getBaseMapper().selectObjs(queryWrapper);
        // 过滤无效数据，转换数据类型
        List<String> stringList = objectList.stream().filter(ObjUtil::isNotNull).map(Object::toString).collect(Collectors.toList());
        // Json转字符串，统计标签次数，标签去重
        Map<String, Long> tagsCountMap = stringList.stream().flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream()).collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 封装为响应类，进行降序排序，返回结果
        List<SpaceTagAnalyzeResponse> list = tagsCountMap.entrySet().stream().sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())).map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        return list;
    }

    /**
     * 查询空间大小分析
     *
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        // 根据查询范围校验权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 根据查询范围填充查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        // 设置查询字段
        queryWrapper.select("picSize");
        // 查询数据库
        List<Long> pictureSizeList = pictureRepository.getBaseMapper().selectObjs(queryWrapper).stream().map(size -> ((Number) size).longValue()).collect(Collectors.toList());
        // 创建一个Map，用于存储图片大小范围及对应的图片数量
        Map<String, Long> pictureSizeMap = new LinkedHashMap<>();
        // 按图片大小的范围分段过滤数据
        pictureSizeMap.put("<100KB", pictureSizeList.stream().filter(size -> size < 100 * 1024).count());
        pictureSizeMap.put("100KB-500KB", pictureSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        pictureSizeMap.put("500KB-1MB", pictureSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        pictureSizeMap.put(">1MB", pictureSizeList.stream().filter(size -> size >= 1024 * 1024).count());
        // 封装响应对象，返回结果
        List<SpaceSizeAnalyzeResponse> list = pictureSizeMap.entrySet().stream().map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        return list;
    }

    /**
     * 查询空间用户上传分析
     *
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        // 权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 根据查询范围填充查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        // 判断时间维度(单位)
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        // 根据时间维度设置查询字段并统计个数
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度错误");
        }
        // 设置分组排序条件
        queryWrapper.groupBy("period").orderByAsc("period");
        // 查询数据库
        List<Map<String, Object>> maps = pictureRepository.getBaseMapper().selectMaps(queryWrapper);
        // 封装为响应对象，返回结果
        List<SpaceUserAnalyzeResponse> list = maps.stream().map(map -> {
            String period = map.get("period").toString();
            long count = ((Number) map.get("count")).longValue();
            return new SpaceUserAnalyzeResponse(period, count);
        }).collect(Collectors.toList());
        return list;
    }

    /**
     * 管理员查询用户空间使用量排行榜
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        // 参数校验
        Integer topN = spaceRankAnalyzeRequest.getTopN();
        ThrowUtils.throwIf(ObjUtil.isEmpty(topN), ErrorCode.PARAMS_ERROR, "排行榜数量不能为空");
        // 仅管理员可查询
        ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // 设置查询字段、排序方式、查询个数
        queryWrapper.select("id", "spaceName", "userId" ,"totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + topN);
        // 查询结果并返回
        return spaceApplicationService.getSpaceList(queryWrapper);
    }


    /**
     * 判断空间分析的查询范围并校验权限
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceAnalyzeRequest), ErrorCode.PARAMS_ERROR, "分析空间参数不能为空");
        // 获取参数
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        // 判断查询范围
        if (queryPublic || queryAll) {
            // 非用户查询范围，校验管理员权限
            ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        } else {
            // 用户查询范围，校验空间是否存在
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(ObjUtil.isEmpty(spaceId), ErrorCode.PARAMS_ERROR, "空间ID不能为空");
            Space space = spaceApplicationService.getSpaceById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验用户权限
            spaceApplicationService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 根据查询范围填充空间分析请求公共查询条件
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        // 全空间，不限制查询范围
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            return;
        }
        // 公共图库
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }
        // 私人空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (ObjUtil.isNotEmpty(spaceId)) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }
}




