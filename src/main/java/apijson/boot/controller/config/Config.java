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

    // 最大查询数量
    public static int maxQueryCount;

    // 最大更新数量
    public static int maxUpdateCount;

    // 最大分页数量
    public static int maxQueryPage;

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

    /**
     * 设置最大查询数量。
     *
     * @param maxQueryCount 最大查询数量
     */
    @Value("${apijson.maxQueryCount}")
    public void setMaxQueryCount(int maxQueryCount) {
        Config.maxQueryCount = maxQueryCount;
    }

    /**
     * 设置最大更新数量。
     *
     * @param maxUpdateCount 最大更新数量
     */
    @Value("${apijson.maxUpdateCount}")
    public void setMaxUpdateCount(int maxUpdateCount) {
        Config.maxUpdateCount = maxUpdateCount;
    }

    /**
     * 设置最大分页数量。
     *
     * @param maxQueryPage 最大分页数量
     */
    @Value("${apijson.maxQueryPage}")
    public void setMaxQueryPage(int maxQueryPage) {
        Config.maxQueryPage = maxQueryPage;
    }
}
