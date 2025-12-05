package yupicture.infrastructure.api.imagesearch.model;

import lombok.Data;

@Data
public class ImageSearchResult {

    /**
     * 缩略图url
     */
    private String thumbUrl;

    /**
     * 源地址
     */
    private String fromUrl;
}
