package com.yupi.yupicturebackend.api.imagesearch;

import com.yupi.yupicturebackend.api.imagesearch.sub.GetImageFirstUrlAPI;
import com.yupi.yupicturebackend.api.imagesearch.model.ImageSearchResult;
import com.yupi.yupicturebackend.api.imagesearch.sub.GetImageList;
import com.yupi.yupicturebackend.api.imagesearch.sub.GetImagePageUrlAPI;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {
    /**
     * 百度识图 API 整合接口入口处
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        // 1. 发送图片获得接口的请求地址
        String imagePageUrl = GetImagePageUrlAPI.getImagePageUrl(imageUrl);
        // 2. 获取这个地址的页面文档，解析出 firstUrl 地址
        String firstUrl = GetImageFirstUrlAPI.getImageFirstUrl(imagePageUrl);
        // 3. 解析 firstUrl 的页面数据，获得图片列表
        List<ImageSearchResult> imageList = GetImageList.getImageList(firstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        String imageUrl = "https://pic.nximg.cn/file/20220830/33331825_091955691124_2.jpg";
        List<ImageSearchResult> imageSearchResults = searchImage(imageUrl);
        System.out.println(imageSearchResults);
    }
}
