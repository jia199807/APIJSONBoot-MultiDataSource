package apijson.boot.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class DataSourceManager {

    // 存储动态数据源的映射
    private static final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    @Autowired
    @Lazy
    private JdbcTemplate jdbcTemplate;

    /**
     * 创建 JdbcTemplate bean，用于数据库操作。
     *
     * @return 配置好的 JdbcTemplate 实例
     */
    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(druidDataSource());
    }

    /**
     * 根据 key 获取数据源。
     *
     * @param key 数据源的名称
     * @return 对应的数据源
     */
    public static DataSource getDataSource(String key) {
        return dataSourceMap.get(key);
    }

    /**
     * 更新数据源映射。
     *
     * @param newMap 新的数据源映射
     */
    public static void updateDataSource(Map<String, DataSource> newMap) {
        dataSourceMap.clear();
        dataSourceMap.putAll(newMap);
    }

    /**
     * 获取所有数据源的映射。
     *
     * @return 当前所有的数据源映射
     */
    public static Map<String, DataSource> getAllDataSources() {
        return new ConcurrentHashMap<>(dataSourceMap);
    }

    /**
     * 配置 Druid 数据源的 bean。
     *
     * @return 配置好的 DruidDataSource 实例
     */
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    @Bean
    public DruidDataSource druidDataSource() {
        return new DruidDataSource();
    }

    /**
     * 配置数据源初始化器，检查表是否存在并执行必要的 SQL 脚本。
     *
     * @return 配置好的 DataSourceInitializer 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DataSourceInitializer dataSourceInitializer() {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(druidDataSource());

        // 检查表是否存在
        String checkTableExistsSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'data_source'";
        Integer count = jdbcTemplate.queryForObject(checkTableExistsSql, Integer.class);

        // 只有在表不存在时才执行 SQL 脚本
        if (count == null || count == 0) {
            System.out.println("相关表不存在，正在执行sys.sql");

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("sys.sql"));
            initializer.setDatabasePopulator(populator);
        } else {
            System.out.println("相关表已存在，跳过sys.sql");
        }

        initializer.setEnabled(true); // 启用初始化器

        // 从数据库加载数据源配置
        loadDataSource();
        return initializer;
    }

    /**
     * 从数据库中加载数据源配置并更新数据源映射。
     * 该方法为同步方法，确保线程安全。
     */
    public synchronized void loadDataSource() {
        Map<String, DataSource> map = new HashMap<>();
        // 使用 JdbcTemplate 查询数据源配置
        String sql = "SELECT pool_name, username, password, url, driver_class_name FROM data_source";
        jdbcTemplate.query(sql, rs -> {
            while (rs.next()) {
                String name = rs.getString("pool_name");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String url = rs.getString("url");
                String driver = rs.getString("driver_class_name");

                DruidDataSource dataSource = new DruidDataSource();
                // 配置数据源的基本属性
                dataSource.setUrl(url);
                dataSource.setUsername(username);
                dataSource.setPassword(password);
                dataSource.setDriverClassName(driver);

                // 配置连接池的属性
                dataSource.setInitialSize(5);
                dataSource.setMaxActive(10);
                dataSource.setMinIdle(2);
                dataSource.setMaxWait(60000);

                dataSource.setValidationQuery("SELECT 1 FROM DUAL");
                dataSource.setTestOnBorrow(true);
                dataSource.setTestWhileIdle(true);
                dataSource.setTimeBetweenEvictionRunsMillis(60000);
                dataSource.setMinEvictableIdleTimeMillis(300000);
                dataSource.setConnectionProperties("druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000");

                map.put(name, dataSource);
            }
        });
        // 更新数据源映射
        updateDataSource(map);
    }

}
