package apijson.boot.controller.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration class for retrieving MySQL database properties.
 */
@Component
public class Config {
    // MySQL数据库账号
    public static String dbAccount;

    // MySQL数据库密码
    public static String dbPassword;

    // MySQL数据库连接URL
    public static String dbUrl;

    // MySQL数据库版本号
    public static String dbVersion;

    /**
     * 设置MySQL数据库账号。
     *
     * @param account MySQL数据库账号
     */
    @Value("${mysql.username}")
    public void setDbAccount(String account) {
        Config.dbAccount = account;
    }

    /**
     * 设置MySQL数据库密码。
     *
     * @param password MySQL数据库密码
     */
    @Value("${mysql.password}")
    public void setDbPassword(String password) {
        Config.dbPassword = password;
    }

    /**
     * 设置MySQL数据库连接URL。
     *
     * @param url MySQL数据库连接URL
     */
    @Value("${mysql.url}")
    public void setDbUrl(String url) {
        Config.dbUrl = url;
    }

    /**
     * 设置MySQL数据库版本号。
     *
     * @param version MySQL数据库版本号
     */
    @Value("${mysql.version}")
    public void setDbVersion(String version) {
        Config.dbVersion = version;
    }
}
