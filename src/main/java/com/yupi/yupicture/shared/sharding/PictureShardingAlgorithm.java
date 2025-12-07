package com.yupi.yupicture.shared.sharding;


import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    /**
     * 分表算法：根据spaceId得出的分表名称进行查询
     * (查分表的名称是什么)
     * @param collection
     * @param preciseShardingValue
     * @return
     */
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Long> preciseShardingValue) {
        /// 分表名算法规则：picture_${spaceId}
        // 获取 spaceId 和逻辑表名
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        if (spaceId == null) {
            // 查询公共图库
            return logicTableName;
        }
        // 根据 spaceId 生成分表名
        String realTableName = logicTableName + "_" + spaceId;
        // 分表集合列表中是否包含该分表
        if (collection.contains(realTableName)) {
            return realTableName;
        } else {
            // 分表不存在则返回逻辑表名
            return logicTableName;
        }
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return List.of();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
