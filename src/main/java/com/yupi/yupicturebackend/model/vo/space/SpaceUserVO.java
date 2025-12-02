package com.yupi.yupicturebackend.model.vo.space;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import com.yupi.yupicturebackend.model.vo.UserVO;
import io.github.classgraph.json.Id;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

import static com.yupi.yupicturebackend.exception.ErrorCode.PARAMS_ERROR;

/**
 * 用户视图（脱敏）
 */
@Data
public class SpaceUserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户信息
     */
    private UserVO userVO;

    /**
     * 空间信息
     */
    private SpaceVO space;

    private static final long serialVersionUID = 1L;

    /**
     * vo 转 entity
     *
     * @param spaceUser
     * @return
     */
    public static SpaceUserVO objToVo(SpaceUser spaceUser) {
        // 校验
        if (ObjUtil.isEmpty(spaceUser)) {
            throw new BusinessException(PARAMS_ERROR, "空间不存在");
        }
        // 拷贝属性
        return BeanUtil.copyProperties(spaceUser, SpaceUserVO.class);
    }

    /**
     * entity 转 vo
     *
     * @param spaceUserVO
     * @return
     */
    public static SpaceUser voToObj(SpaceUserVO spaceUserVO) {
        // 校验
        if (ObjUtil.isEmpty(spaceUserVO)) {
            throw new BusinessException(PARAMS_ERROR, "空间不存在");
        }
        // 拷贝属性
        return BeanUtil.copyProperties(spaceUserVO, SpaceUser.class);
    }
}
