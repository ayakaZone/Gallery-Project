package yupicture.interfaces.vo.picture;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import yupicture.infrastructure.exception.BusinessException;
import yupicture.domain.picture.entity.Picture;
import lombok.Data;
import yupicture.interfaces.vo.user.UserVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static yupicture.infrastructure.exception.ErrorCode.PARAMS_ERROR;

@Data
public class PictureVO implements Serializable {

    private static final long serialVersionUID = -4195602091437156856L;

    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;


    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 上传图片的用户
     */
    private UserVO user;

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
     * 空间 id
     */
    private Long spaceId;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    /**
     * vo 转 entity
     *
     * @param picture
     * @return
     */
    public static PictureVO objToVo(Picture picture) {
        // 校验
        if (ObjUtil.isEmpty(picture)) {
            throw new BusinessException(PARAMS_ERROR, "图片不存在");
        }
        // 拷贝属性
        PictureVO pictureVO = BeanUtil.copyProperties(picture, PictureVO.class);
        // 把 JSON 数组转为 List
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
    }

    /**
     * entity 转 vo
     *
     * @param pictureVO
     * @return
     */
    public static Picture voToObj(PictureVO pictureVO) {
        // 校验
        if (ObjUtil.isEmpty(pictureVO)) {
            throw new BusinessException(PARAMS_ERROR, "图片不存在");
        }
        // 拷贝属性
        Picture picture = BeanUtil.copyProperties(pictureVO, Picture.class);
        // 把 List 数组转为 JsonStr
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }
}
