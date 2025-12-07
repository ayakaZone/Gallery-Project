package com.yupi.yupicture.shared.sharding;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
//@Component
public class DynamicShardingManager {

    @Resource
    private DataSource dataSource;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    private final String LOGIC_TABLE_NAME = "picture";

    private static final String DATABASE_NAME = "logic_db";

    @PostConstruct
    private void initialize() {
        log.info("初始化动态分表配置...");
        updateShardingTableNodes();

    }

    /**
     * 获取初始表（逻辑表）picture，和所有的分表 picture_${spaceId}
     *
     * @return
     */
    private Set<String> fetchAllPictureTableNames() {
        // 查询所有空间类型为团队空间的spaceId
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<Space>()
                .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue());
        List<Space> teamSpaces = spaceApplicationService.lambdaQuerySpace(queryWrapper);
        Set<Long> spaceIdSet = teamSpaces.stream().map(Space::getId).collect(Collectors.toSet());
        // 拼接所有的分表名称
        Set<String> tableNameList = spaceIdSet.stream().map(spaceId -> LOGIC_TABLE_NAME + "_" + spaceId).collect(Collectors.toSet());
        // 加上初始表（逻辑表）
        tableNameList.add(LOGIC_TABLE_NAME);
        return tableNameList;
    }

    /**
     * 动态更新 ShardingSphere 的 actual-data-nodes 配置项（真正查询的表）
     */
    private void updateShardingTableNodes() {
        // 获取所有的分表列表
        Set<String> tableNameList = fetchAllPictureTableNames();
        // 为所有分表名加上前缀 yu_picture （数据库名称）
        String tableNameStr = tableNameList.stream().map(tableName -> "yu_picture." + tableName).collect(Collectors.joining(","));
        log.info("待更新分表节点：{}", tableNameStr);
        // 获取 ShardingSphere 的上下文管理器
        ContextManager contextManager = this.getContextManager();
        // 找到ShardingSphere的Role配置项
        ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts().getMetaData().getDatabases().get(DATABASE_NAME).getRuleMetaData();
        Optional<ShardingRule> shardingRule = ruleMetaData.findSingleRule(ShardingRule.class);
        if (shardingRule.isPresent()) {
            ShardingRuleConfiguration ruleConfig = (ShardingRuleConfiguration) shardingRule.get().getConfiguration();
            List<ShardingTableRuleConfiguration> updatedRules = ruleConfig.getTables().stream().map(oldTableRule -> {
                if (LOGIC_TABLE_NAME.equals(oldTableRule.getLogicTable())) {
                    // 这里就是更新 actual-data-nodes 配置项
                    ShardingTableRuleConfiguration newTableRuleConfig =
                            new ShardingTableRuleConfiguration(LOGIC_TABLE_NAME, tableNameStr);
                    // 下面都是样板代码
                    newTableRuleConfig.setDatabaseShardingStrategy(oldTableRule.getDatabaseShardingStrategy());
                    newTableRuleConfig.setTableShardingStrategy(oldTableRule.getTableShardingStrategy());
                    newTableRuleConfig.setKeyGenerateStrategy(oldTableRule.getKeyGenerateStrategy());
                    newTableRuleConfig.setAuditStrategy(oldTableRule.getAuditStrategy());
                    return newTableRuleConfig;
                }
                return oldTableRule;
            }).collect(Collectors.toList());
            ruleConfig.setTables(updatedRules);
            contextManager.alterRuleConfiguration(DATABASE_NAME, Collections.singleton(ruleConfig));
            contextManager.reloadDatabase(DATABASE_NAME);
            log.info("动态分表规则更新成功！");
        } else {
            log.error("未找到 ShardingSphere 的分片规则配置，动态分表更新失败。");
        }
    }

    /**
     * 获取 ShardingSphere 的上下文管理器
     *
     * @return
     */
    private ContextManager getContextManager() {
        try (ShardingSphereConnection connection = dataSource.getConnection().unwrap(ShardingSphereConnection.class)) {
            return connection.getContextManager();
        } catch (Exception e) {
            throw new RuntimeException("获取 ShardingSphere 的上下文管理器失败", e);
        }
    }

    /**
     * 动态创建分表
     *
     * @param space
     */
    public void createSpacePictureTable(Space space) {
        // 仅为旗舰版团队空间创建分表
        if (space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue()) && space.getSpaceLevel().equals(SpaceLevelEnum.FLAGSHIP.getValue())) {
            // 获取 spaceId
            Long spaceId = space.getId();
            // 拼接分表名
            String tableName = LOGIC_TABLE_NAME + "_" + spaceId;
            // 创建分表SQL
            String createTableSql = "CREATE TABLE " + tableName + " LIKE picture";
            try {
                // 动态SQL执行器执行创建分表SQL
                SqlRunner.db().update(createTableSql);
                // 更新配置类 actual-data-nodes 的分表名称列表
                updateShardingTableNodes();
            } catch (Exception e) {
                log.error("创建分表失败：{}, 空间Id：{}", e.getMessage(), spaceId);
            }
        }
    }
}


