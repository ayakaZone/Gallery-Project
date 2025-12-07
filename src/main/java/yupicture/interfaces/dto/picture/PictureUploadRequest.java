package yupicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = -5249359046041845680L;

    /**
     * 图片ID
     */
    private Long id;

    /**
     * 图片Url
     */
    private String fileUrl;

    /**
     * 图片名
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;

}
