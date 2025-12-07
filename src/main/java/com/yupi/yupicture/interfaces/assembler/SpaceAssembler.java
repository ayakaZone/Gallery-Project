package com.yupi.yupicture.interfaces.assembler;

import cn.hutool.core.bean.BeanUtil;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.interfaces.dto.space.SpaceEditRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceUpdateRequest;

public class SpaceAssembler {

    public static Space toSpaceEntity(SpaceUpdateRequest spaceUpdateRequest) {
        return BeanUtil.copyProperties(spaceUpdateRequest, Space.class);
    }

    public static Space toSpaceEntity(SpaceEditRequest spaceEditRequest) {
        return BeanUtil.copyProperties(spaceEditRequest, Space.class);
    }
}

