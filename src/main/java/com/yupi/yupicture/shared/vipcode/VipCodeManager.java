package com.yupi.yupicture.shared.vipcode;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

@Component
public class VipCodeManager {

    private static final String JSON_FILE = "riz/vipCode.json";

    /**
     * 验证并兑换码（原子操作）
     */
    public synchronized boolean validateAndUseCode(String vipCode) {
        File file = new File(JSON_FILE);
        if (!file.exists()) {
            return false;
        }

        try {
            // 1. 读取JSON文件
            String json = FileUtil.readUtf8String(file);
            Map<String, Object> data = JSONUtil.toBean(json, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> codes = (List<Map<String, Object>>) data.get("codes");

            if (codes == null) {
                return false;
            }

            // 2. 查找兑换码
            boolean found = false;
            for (Map<String, Object> code : codes) {
                String codeStr = (String) code.get("vipCode");
                if (vipCode.equals(codeStr)) {
                    Boolean hasUsed = (Boolean) code.get("hasUsed");

                    if (Boolean.TRUE.equals(hasUsed)) {
                        return false;
                    }

                    // 3. 标记为已使用
                    code.put("hasUsed", true);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }

            // 4. 保存回文件
            data.put("codes", codes);
            String newJson = JSONUtil.toJsonPrettyStr(data);
            FileUtil.writeUtf8String(newJson, file);

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}