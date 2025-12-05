package yupicture.infrastructure.api.imagesearch.sub;

import yupicture.infrastructure.exception.BusinessException;
import yupicture.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GetImageFirstUrlAPI {

    /**
     * 解析页面数据，获得存放图片的 firstUrl
     * @param url
     * @return
     */
    public static String getImageFirstUrl(String url) {
        try {
            // 根据 url 获取页面文档
            Document document = Jsoup.connect(url).timeout(5000).get();
            // 获取 <script> 标签的所有内容
            Elements script = document.getElementsByTag("script");
            // 遍历查找 firstUrl 标签
            for (Element element : script) {
                String scriptContent = element.html();
                if (scriptContent.contains("\"firstUrl\"")) {
                    // 获取 firstUrl 的值
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\"(.*?)\"");
                    Matcher matcher = pattern.matcher(scriptContent);
                    if (matcher.find()) {
                        // 0 表示获得整个匹配的字符串
                        String firstUrl = matcher.group(1);
                        // 处理转义
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到 firstUrl");
        } catch (IOException e) {
            log.info("获取图片链接失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片链接失败");
        }
    }

    public static void main(String[] args) {
        String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&f=all" +
                "&isLogoShow=1&session_id=14228129104275619068&sign=12627f83cfd8baa33baaf01764428509&tpl_from=pc";
        String imageUrl = getImageFirstUrl(url);
        System.out.println(imageUrl);
    }
}
