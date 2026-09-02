-- ============================================================
-- 更新采购入库表结构以支持关联采购订单
-- 文件: V3__update_inbound.sql
-- 描述: 添加doc_type和doc_id字段以实现采购订单与入库单的关联
-- ============================================================

-- 为purchase_inbound表添加关联字段
ALTER TABLE purchase_inbound ADD doc_type NVARCHAR(20) NULL; -- PURCHASE_ORDER / PURCHASE_RETURN
ALTER TABLE purchase_inbound ADD doc_id BIGINT NULL;      -- 来源单据ID，如采购订单ID
GO

-- 为doc_type和doc_id添加索引
CREATE INDEX idx_inbound_doc ON purchase_inbound (doc_type, doc_id);
GO

-- 更新现有数据，如果有需要的话
-- 示例：如果有现有的入库单需要关联到采购订单，可以在这里更新
-- UPDATE purchase_inbound SET doc_type = 'PURCHASE_ORDER', doc_id = [对应的采购订单ID] WHERE [条件];
GO