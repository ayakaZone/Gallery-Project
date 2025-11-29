package com.yupi.yupicturebackend.utils;

import java.awt.*;

public class ColorSimilarUtils {

    private ColorSimilarUtils() {
    }

    /**
     * 计算两个颜色的相似度
     *
     * @param color1
     * @param color2
     * @return
     */
    public static double getSimilarity(Color color1, Color color2) {
        // 获取颜色的 RGB 值
        int red1 = color1.getRed();
        int blue1 = color1.getBlue();
        int green1 = color1.getGreen();

        int red2 = color2.getRed();
        int blue2 = color2.getBlue();
        int green2 = color2.getGreen();

        // 计算欧式距离
        double distance = Math.sqrt(Math.pow(red1 - red2, 2) + Math.pow(blue1 - blue2, 2) + Math.pow(green1 - green2, 2));

        // 计算相似度
        return 1 - distance / (3 * Math.pow(255, 2));
    }

    /**
     * 根据16进制计算颜色相似度
     *
     * @param color1
     * @param color2
     * @return
     */
    public static double getSimilarity(String color1, String color2) {
        // 创建 Color 对象
        Color c1 = Color.decode(color1);
        Color c2 = Color.decode(color2);

        // 调用 getSimilarity 方法计算相似度
        return getSimilarity(c1, c2);
    }

    /**
     * 测试用例
     *
     * @param args
     */
    public static void main(String[] args) {
        String color1 = "#FF0000";
        String color2 = "#00FF00";

        Color c1 = Color.decode(color1);
        Color c2 = Color.decode(color2);

        System.out.println("相似度：" + getSimilarity(c1, c2));
        System.out.println("相似度：" + getSimilarity(c1, c2));
    }
}
