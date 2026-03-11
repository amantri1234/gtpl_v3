package com.gtplug.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Application properties loaded successfully");
            } else {
                logger.warn("application.properties not found, using environment variables");
            }
        } catch (IOException e) {
            logger.error("Error loading properties: {}", e.getMessage());
        }
    }

    private static String getProperty(String key, String defaultValue) {
        String envValue = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    public static synchronized DataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            HikariConfig config = new HikariConfig();
            
            config.setDriverClassName(getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
            config.setJdbcUrl(getProperty("db.url", "jdbc:mysql://localhost:3306/gtplug_db"));
            config.setUsername(getProperty("db.username", "root"));
            config.setPassword(getProperty("db.password", "root"));
            
            // Pool configuration
            config.setMaximumPoolSize(Integer.parseInt(getProperty("db.pool.size.max", "20")));
            config.setMinimumIdle(Integer.parseInt(getProperty("db.pool.size.min", "5")));
            config.setConnectionTimeout(Long.parseLong(getProperty("db.pool.connection.timeout", "30000")));
            config.setIdleTimeout(Long.parseLong(getProperty("db.pool.idle.timeout", "600000")));
            config.setMaxLifetime(Long.parseLong(getProperty("db.pool.max.lifetime", "1800000")));
            config.setLeakDetectionThreshold(Long.parseLong(getProperty("db.pool.leak.detection.threshold", "60000")));
            
            // Additional optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            // Connection test
            config.setConnectionTestQuery("SELECT 1");
            
            dataSource = new HikariDataSource(config);
            logger.info("HikariCP connection pool initialized successfully");
        }
        return dataSource;
    }

    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool closed");
        }
    }

    public static String getJwtSecret() {
        return getProperty("jwt.secret", "gtplugSuperSecretKeyForJwtTokenGeneration2024");
    }

    public static long getJwtExpiration() {
        return Long.parseLong(getProperty("jwt.expiration", "86400000"));
    }

    public static String getUploadDirectory() {
        return getProperty("upload.directory", "./uploads");
    }

    public static int getRateLimitRequests() {
        return Integer.parseInt(getProperty("security.rate.limit.requests", "100"));
    }

    public static long getRateLimitWindow() {
        return Long.parseLong(getProperty("security.rate.limit.window", "60000"));
    }

    public static long getSessionTimeout() {
        return Long.parseLong(getProperty("security.session.timeout", "3600000"));
    }

    public static int getMaxFailedLoginAttempts() {
        return Integer.parseInt(getProperty("security.account.lock.attempts", "5"));
    }

    public static long getAccountLockDuration() {
        return Long.parseLong(getProperty("security.account.lock.duration", "1800000"));
    }
}
