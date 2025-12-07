package yupicture.interfaces.vo.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 脱敏用户
 * @TableName user
 */
@Data
public class PictureTagCategory implements Serializable {

    /**
     * 分类
     */
    private List<String> categoryList;

    /**
     * 标签
     */
    private List<String> tagList;

    private static final long serialVersionUID = 1L;
}
