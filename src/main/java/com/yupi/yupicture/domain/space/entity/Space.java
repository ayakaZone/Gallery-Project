package com.yupi.yupicture.domain.space.entity;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import com.yupi.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;

import java.util.Date;

/**
 * 空间
 *
 * @TableName space
 */
@TableName(value = "space")
@Data
public class Space {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间类型：0-个人空间 1-团队空间
     */
    private Integer spaceType;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 空间参数校验
     *
     * @param add
     */
    public void validSpace(boolean add) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(this), ErrorCode.PARAMS_ERROR);
        // 获取参数
        String spaceName = this.getSpaceName();
        Integer spaceLevel = this.getSpaceLevel();
        Integer spaceType = this.getSpaceType();
        // 获取枚举说明
        SpaceLevelEnum spaceLevelEnumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnumValue = SpaceTypeEnum.getEnumByValue(spaceType);
        // 增加空间还是更新空间
        if (add) {
            // 增加：名称不为空，级别不为空
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (ObjUtil.isEmpty(spaceLevel)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            // 空间类型
            if (ObjUtil.isEmpty(spaceType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }
        // 更新：名称不为空，级别不为空，名称小于30字符
        if (ObjUtil.isNotEmpty(spaceLevel) && ObjUtil.isEmpty(spaceLevelEnumByValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能超过30个字符");
        }
        if (ObjUtil.isNotEmpty(spaceType) && ObjUtil.isEmpty(spaceTypeEnumValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }

    }
}