package yupicture.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByPictureRequest implements Serializable {


    private static final long serialVersionUID = -7502219825472679547L;

    /**
     * 图片ID
     */
    private Long pictureId;
}
