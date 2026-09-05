package com.erp.module.system.service;

import com.erp.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Creates and lists SQL Server database backups in a server-controlled directory. */
@Service
@RequiredArgsConstructor
public class BackupService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final JdbcTemplate jdbcTemplate;
    @Value("${erp.backup.directory:./backups}")
    private String directory;
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    public record BackupInfo(String fileName, long size, LocalDateTime createdAt) {}

    public BackupInfo create() {
        Path dir = backupDirectory();
        try { Files.createDirectories(dir); } catch (IOException e) { throw new BusinessException("无法创建备份目录"); }
        String database = databaseName();
        String fileName = "erp-" + FILE_TIME.format(LocalDateTime.now()) + "-" + System.nanoTime() + ".bak";
        Path target = dir.resolve(fileName).normalize();
        if (!target.startsWith(dir)) throw new BusinessException("备份文件路径无效");
        String escapedDatabase = database.replace("]", "]]" );
        String escapedPath = target.toString().replace("'", "''");
        try {
            jdbcTemplate.execute("BACKUP DATABASE [" + escapedDatabase + "] TO DISK = '" + escapedPath + "' WITH INIT, CHECKSUM, STATS = 10");
            try {
                return info(target, fileName);
            } catch (IOException e) {
                try { Files.deleteIfExists(target); } catch (IOException ignored) { }
                throw new BusinessException("无法读取备份文件信息");
            }
        } catch (RuntimeException e) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new BusinessException("数据库备份失败: " + (e.getMessage() == null ? "未知错误" : e.getMessage()));
        }
    }

    public List<BackupInfo> list() {
        Path dir = backupDirectory();
        if (!Files.isDirectory(dir)) return List.of();
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".bak"))
                    .map(p -> { try { return info(p, p.getFileName().toString()); } catch (IOException e) { return null; } })
                    .filter(java.util.Objects::nonNull).sorted(Comparator.comparing(BackupInfo::createdAt).reversed()).toList();
        } catch (IOException e) { throw new BusinessException("无法读取备份目录"); }
    }

    public Path file(String fileName) {
        if (fileName == null || !fileName.matches("erp-[0-9]{8}-[0-9]{6}-[0-9]+\\.bak")) throw new BusinessException(400, "备份文件名无效");
        Path dir = backupDirectory(); Path file = dir.resolve(fileName).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) throw new BusinessException(404, "备份文件不存在");
        return file;
    }

    private BackupInfo info(Path path, String fileName) throws IOException { return new BackupInfo(fileName, Files.size(path), Files.getLastModifiedTime(path).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()); }
    private Path backupDirectory() { return Paths.get(directory).toAbsolutePath().normalize(); }
    private String databaseName() {
        String value = datasourceUrl == null ? "" : datasourceUrl;
        for (String part : value.split(";")) if (part.toLowerCase().startsWith("databasename=")) return part.substring(part.indexOf('=') + 1).trim();
        throw new BusinessException("数据源未配置数据库名称");
    }
}
