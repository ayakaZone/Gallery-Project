package yupicture.interfaces.assembler;

import cn.hutool.core.bean.BeanUtil;
import yupicture.domain.picture.entity.Picture;
import yupicture.interfaces.dto.picture.PictureEditRequest;
import yupicture.interfaces.dto.picture.PictureUpdateRequest;

public class PictureAssembler {

    public static Picture toPictureEntity(PictureEditRequest pictureEditRequest) {
        // 封装user
        return BeanUtil.copyProperties(pictureEditRequest, Picture.class);
    }

    public static Picture toPictureEntity(PictureUpdateRequest pictureUpdateRequest) {
        // 封装user
        return BeanUtil.copyProperties(pictureUpdateRequest, Picture.class);
    }
}

