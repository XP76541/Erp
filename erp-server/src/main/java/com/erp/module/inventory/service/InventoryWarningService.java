package com.erp.module.inventory.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import com.erp.module.inventory.entity.InventoryWarning;
import com.erp.module.inventory.entity.InventoryWarningConfig;
import com.erp.module.inventory.mapper.InventoryWarningMapper;
import com.erp.module.inventory.mapper.InventoryWarningConfigMapper;
import com.erp.module.inventory.dto.InventoryWarningDtos;
import com.erp.module.system.TokenStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存预警服务
 */
@Service
public class InventoryWarningService {

    private final InventoryWarningMapper warningMapper;
    private final InventoryWarningConfigMapper configMapper;
    private final WarehouseMapper warehouseMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;

    public InventoryWarningService(InventoryWarningMapper warningMapper,
                                  InventoryWarningConfigMapper configMapper,
                                  WarehouseMapper warehouseMapper,
                                  ProductMapper productMapper,
                                  InventoryService inventoryService) {
        this.warningMapper = warningMapper;
        this.configMapper = configMapper;
        this.warehouseMapper = warehouseMapper;
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
    }

    /** 分页查询:按类型/仓库/商品过滤 */
    public PageResult<InventoryWarningDtos.ListResponse> page(long page, long size,
            String warningType, Long warehouseId, Long productId, Boolean isActive) {
        Page<InventoryWarning> result = warningMapper.selectPage(new Page<>(page, size),
                Wrappers.<InventoryWarning>lambdaQuery()
                        .eq(StringUtils.hasText(warningType), InventoryWarning::getWarningType, warningType)
                        .eq(warehouseId != null, InventoryWarning::getWarehouseId, warehouseId)
                        .eq(productId != null, InventoryWarning::getProductId, productId)
                        .eq(isActive != null, InventoryWarning::getIsActive, isActive)
                        .orderByDesc(InventoryWarning::getId));

        List<InventoryWarningDtos.ListResponse> responses = result.getRecords().stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), responses);
    }

    /** 预警详情 */
    public InventoryWarningDtos.DetailResponse detail(Long id) {
        InventoryWarning warning = requireWarning(id);

        // 获取预警日志
        List<InventoryWarningDtos.WarningLog> logs = getWarningLogs(id);

        return new InventoryWarningDtos.DetailResponse(warning, logs);
    }

    /** 获取激活的预警 */
    public List<InventoryWarningDtos.ActiveResponse> getActiveWarnings(Long warehouseId) {
        List<InventoryWarning> warnings = warningMapper.selectAllActiveWarnings(
                warehouseId != null ? warehouseId : null);

        return warnings.stream()
                .map(warning -> {
                    InventoryWarningDtos.ActiveResponse response = new InventoryWarningDtos.ActiveResponse(warning);

                    // 设置预警级别
                    InventoryWarningConfig config = configMapper.selectByProductAndWarehouse(
                            warning.getProductId(), warning.getWarehouseId()).stream()
                            .findFirst()
                            .orElse(null);

                    if (config != null) {
                        response.setWarningLevel(config.getWarningLevel());
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    /** 根据预警类型查询 */
    public List<InventoryWarningDtos.ActiveResponse> getWarningsByType(String warningType, Long warehouseId) {
        List<InventoryWarning> warnings = warningMapper.selectActiveWarnings(warningType, warehouseId);

        return warnings.stream()
                .map(this::toActiveResponse)
                .collect(Collectors.toList());
    }

    /** 解决预警 */
    @Transactional
    public void resolveWarning(Long id, TokenStore.LoginUser user, String remark) {
        InventoryWarning warning = requireWarning(id);
        if (!warning.getIsActive()) {
            throw new BusinessException("预警已解决");
        }

        warningMapper.resolveWarning(id, user.userId());

        // 记录解决日志
        InventoryWarningDtos.WarningLog log = new InventoryWarningDtos.WarningLog();
        log.setWarningId(id);
        log.setOldQty(warning.getCurrentQty());
        log.setNewQty(BigDecimal.ZERO);
        log.setOperatorId(user.userId());
        log.setOperationTime(LocalDateTime.now());
        log.setOperationType("RESOLVE");
        log.setRemark(remark);

        addWarningLog(log);
    }

    /** 批量解决预警 */
    @Transactional
    public void batchResolveWarnings(List<Long> ids, TokenStore.LoginUser user) {
        for (Long id : ids) {
            resolveWarning(id, user, "批量解决");
        }
    }

    /** 预警统计 */
    public InventoryWarningDtos.StatsResponse getStats() {
        // 库存不足预警
        Integer stockOutCount = warningMapper.selectActiveWarnings("STOCK_OUT", null).size();

        // 库存超量预警
        Integer stockOverCount = warningMapper.selectActiveWarnings("STOCK_OVER", null).size();

        // 临期预警
        Integer expiringCount = warningMapper.selectActiveWarnings("EXPIRING", null).size();

        // 呆滞预警
        Integer spoiledCount = warningMapper.selectActiveWarnings("SPOILED", null).size();

        // 总金额
        BigDecimal totalAmount = calculateTotalWarningAmount();

        return new InventoryWarningDtos.StatsResponse(
                stockOutCount, stockOverCount, expiringCount, spoiledCount, totalAmount);
    }

    /** 获取逾期未解决的预警 */
    public List<InventoryWarningDtos.OverdueResponse> getOverdueWarnings() {
        List<InventoryWarning> warnings = warningMapper.selectOverdueWarnings();

        return warnings.stream()
                .map(warning -> {
                    InventoryWarningDtos.OverdueResponse response = new InventoryWarningDtos.OverdueResponse(warning);
                    response.setProductName(getProductName(warning.getProductId()));
                    response.setWarehouseName(getWarehouseName(warning.getWarehouseId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    /** 获取预警配置 */
    public List<InventoryWarningDtos.WarningConfigResponse> getWarningConfigs(Long productId, Long warehouseId) {
        List<InventoryWarningConfig> configs = configMapper.selectByProductAndWarehouse(productId, warehouseId);

        return configs.stream()
                .map(config -> {
                    InventoryWarningDtos.WarningConfigResponse response =
                            new InventoryWarningDtos.WarningConfigResponse(config);
                    response.setProductName(getProductName(config.getProductId()));
                    response.setWarehouseName(getWarehouseName(config.getWarehouseId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    /** 创建预警配置 */
    @Transactional
    public Long createWarningConfig(InventoryWarningDtos.CreateConfigRequest request, TokenStore.LoginUser user) {
        // 验证商品和仓库
        Product product = productMapper.selectById(request.getProductId());
        if (product == null || product.getIsActive() == 0) {
            throw new BusinessException("商品不存在或已停用");
        }

        Warehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null || warehouse.getIsActive() == 0) {
            throw new BusinessException("仓库不存在或已停用");
        }

        // 检查是否已存在配置
        List<InventoryWarningConfig> existing = configMapper.selectByProductAndWarehouse(
                request.getProductId(), request.getWarehouseId());
        if (!existing.isEmpty()) {
            throw new BusinessException("该商品在该仓库的预警配置已存在");
        }

        InventoryWarningConfig config = new InventoryWarningConfig();
        config.setProductId(request.getProductId());
        config.setWarehouseId(request.getWarehouseId());
        config.setStockOutLimit(request.getStockOutLimit() != null ? request.getStockOutLimit() : BigDecimal.ZERO);
        config.setStockOverLimit(request.getStockOverLimit() != null ? request.getStockOverLimit() : BigDecimal.ZERO);
        config.setWarningLevel(request.getWarningLevel() != null ? request.getWarningLevel() : "NORMAL");
        config.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        configMapper.insert(config);
        return config.getId();
    }

    /** 更新预警配置 */
    @Transactional
    public void updateWarningConfig(Long id, InventoryWarningDtos.UpdateConfigRequest request, TokenStore.LoginUser user) {
        InventoryWarningConfig config = requireConfig(id);

        config.setStockOutLimit(request.getStockOutLimit() != null ? request.getStockOutLimit() : config.getStockOutLimit());
        config.setStockOverLimit(request.getStockOverLimit() != null ? request.getStockOverLimit() : config.getStockOverLimit());
        config.setWarningLevel(request.getWarningLevel() != null ? request.getWarningLevel() : config.getWarningLevel());
        config.setIsActive(request.getIsActive() != null ? request.getIsActive() : config.getIsActive());

        configMapper.updateById(config);
    }

    /** 启用/禁用预警配置 */
    @Transactional
    public void toggleWarningConfig(Long id, Boolean isActive, TokenStore.LoginUser user) {
        InventoryWarningConfig config = requireConfig(id);
        config.setIsActive(isActive);
        configMapper.updateById(config);
    }

    /** 批量启用/禁用预警配置 */
    @Transactional
    public void batchToggleWarningConfig(List<Long> ids, Boolean isActive, TokenStore.LoginUser user) {
        configMapper.batchUpdateActiveStatus(ids, isActive);
    }

    /** 检查并生成预警 */
    @Transactional
    public void checkAndGenerateWarnings() {
        // 获取所有激活的预警配置
        List<InventoryWarningConfig> configs = configMapper.selectList(Wrappers.emptyWrapper());

        for (InventoryWarningConfig config : configs) {
            if (!config.getIsActive()) {
                continue;
            }

            BigDecimal currentStock = inventoryService.getStockQuantity(
                    config.getProductId(), config.getWarehouseId());

            // 检查库存不足预警
            if (currentStock.compareTo(config.getStockOutLimit()) < 0) {
                createOrUpdateWarning("STOCK_OUT", config.getProductId(), config.getWarehouseId(),
                        currentStock, config.getStockOutLimit());
            }

            // 检查库存超量预警
            if (currentStock.compareTo(config.getStockOverLimit()) > 0) {
                createOrUpdateWarning("STOCK_OVER", config.getProductId(), config.getWarehouseId(),
                        currentStock, config.getStockOverLimit());
            }
        }
    }

    // 私有辅助方法
    private InventoryWarningDtos.ListResponse toListResponse(InventoryWarning warning) {
        InventoryWarningDtos.ListResponse response = new InventoryWarningDtos.ListResponse(warning);
        response.setProductName(getProductName(warning.getProductId()));
        response.setWarehouseName(getWarehouseName(warning.getWarehouseId()));
        return response;
    }

    private InventoryWarningDtos.ActiveResponse toActiveResponse(InventoryWarning warning) {
        InventoryWarningDtos.ActiveResponse response = new InventoryWarningDtos.ActiveResponse(warning);
        response.setProductName(getProductName(warning.getProductId()));
        response.setWarehouseName(getWarehouseName(warning.getWarehouseId()));

        // 设置预警级别
        InventoryWarningConfig config = configMapper.selectByProductAndWarehouse(
                warning.getProductId(), warning.getWarehouseId()).stream()
                .findFirst()
                .orElse(null);

        if (config != null) {
            response.setWarningLevel(config.getWarningLevel());
        }

        return response;
    }

    private void createOrUpdateWarning(String warningType, Long productId, Long warehouseId,
                                     BigDecimal currentQty, BigDecimal warningValue) {
        // 检查是否已存在激活的预警
        List<InventoryWarning> existing = warningMapper.selectActiveWarningsByProduct(productId, warehouseId);

        if (existing.isEmpty()) {
            // 创建新预警
            InventoryWarning warning = new InventoryWarning();
            warning.setWarningType(warningType);
            warning.setWarehouseId(warehouseId);
            warning.setProductId(productId);
            warning.setCurrentQty(currentQty);
            warning.setWarningValue(warningValue);
            warning.setIsActive(true);
            warningMapper.insert(warning);
        } else {
            // 更新现有预警的数量
            InventoryWarning warning = existing.get(0);
            warning.setCurrentQty(currentQty);
            warningMapper.updateById(warning);
        }
    }

    private BigDecimal calculateTotalWarningAmount() {
        // 计算所有激活预警的总金额
        List<InventoryWarning> warnings = warningMapper.selectAllActiveWarnings(null);

        return warnings.stream()
                .map(warning -> {
                    BigDecimal amount = warning.getCurrentQty().multiply(
                            getProductPrice(warning.getProductId()));
                    return amount.compareTo(BigDecimal.ZERO) > 0 ? amount : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getProductPrice(Long productId) {
        Product product = productMapper.selectById(productId);
        return product != null && product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO;
    }

    private String getProductName(Long productId) {
        Product product = productMapper.selectById(productId);
        return product != null ? product.getName() : "";
    }

    private String getWarehouseName(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        return warehouse != null ? warehouse.getName() : "";
    }

    private List<InventoryWarningDtos.WarningLog> getWarningLogs(Long warningId) {
        // TODO: 需要实现 InventoryWarningLogMapper 和查询方法
        // 暂时返回空列表
        return List.of();
    }

    private void addWarningLog(InventoryWarningDtos.WarningLog log) {
        // TODO: 需要实现 InventoryWarningLog 的保存方法
    }

    private InventoryWarning requireWarning(Long id) {
        InventoryWarning warning = warningMapper.selectById(id);
        if (warning == null) {
            throw new BusinessException("库存预警不存在");
        }
        return warning;
    }

    private InventoryWarningConfig requireConfig(Long id) {
        InventoryWarningConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("预警配置不存在");
        }
        return config;
    }
}