package com.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 小型贸易公司 ERP 服务端
 * 模块划分与表结构见 docs/database-design.md
 */
@SpringBootApplication
@MapperScan({"com.erp.module.system.mapper", "com.erp.module.masterdata.mapper"})
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
}
