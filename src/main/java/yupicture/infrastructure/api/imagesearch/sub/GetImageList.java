package yupicture.infrastructure.api.imagesearch.sub;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import yupicture.infrastructure.api.imagesearch.model.ImageSearchResult;
import yupicture.infrastructure.exception.BusinessException;
import yupicture.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GetImageList {

    /**
     * 根据之前获得的 firstUrl 解析页面获取图片列表
     * @param url
     * @return
     */
    public static List<ImageSearchResult> getImageList(String url) {
        // 向 firstUrl 发送 GET 请求获得图片页面文档
        HttpResponse httpResponse = HttpUtil.createGet(url).execute();
        // 响应码
        int status = httpResponse.getStatus();
        // 响应体
        String body = httpResponse.body();
        if (status == 200) {
            return proccessResponse(body);
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }
    }

    /**
     * 处理响应体，获取图片列表
     * @param body
     * @return
     */
    private static List<ImageSearchResult> proccessResponse(String body) {
        JSONObject jsonObject = new JSONObject(body);
        if (!jsonObject.containsKey("data")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }
        JSONObject data = jsonObject.getJSONObject("data");
        if (!data.containsKey("list")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }
        JSONArray list = data.getJSONArray("list");
        return JSONUtil.toList(list, ImageSearchResult.class);
    }

    public static void main(String[] args) {
        String url = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%" +
                "5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=1" +
                "4228129104275619068&sign=12627f83cfd8baa33baaf01764428509&tk=74808&tpl_from=pc";
        List<ImageSearchResult> imageList = getImageList(url);
        System.out.println(imageList);
    }
}
