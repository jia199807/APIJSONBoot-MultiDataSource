/*Copyright ©2016 TommyLemon(https://github.com/TommyLemon/APIJSON)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package apijson.boot.config;

import apijson.demo.DemoSQLConfig;
import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.HashMap;
import java.util.Map;


/**
 * 数据源配置，对应 application.yml 的数据库连接池 datasource 配置
 *
 * @author Lemon
 */
@Configuration
@Import(DemoSQLConfig.class)
public class DemoDataSourceConfig {


    @Autowired
    private DemoSQLConfig demoSQLConfig;


    @Autowired
    @Lazy
    private JdbcTemplate jdbcTemplate;

    @Bean
    // @DependsOn("datasource")
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(druidDataSource());
    }

    /**
     * 数据源配置，对应 application.yml 的数据库连接池 datasource 配置。
     * 也可以直接 new 再 set 属性，具体见 HikariCP 的文档
     * https://github.com/brettwooldridge/HikariCP#rocket-initialization
     */
    // @Bean
    // @ConfigurationProperties(prefix = "spring.datasource.hikari")
    // public HikariDataSource hikaricpDataSource() {
    //     return new HikariDataSource();
    // }


    /**
     * 数据源配置，对应 application.yml 的数据库连接池 datasource 配置。
     * 也可以直接 new 再 set 属性，具体见 Druid 的 DbTestCase
     * https://github.com/alibaba/druid/blob/master/src/test/java/com/alibaba/druid/DbTestCase.java
     *
     * @author Lemon
     */
    // Need to be specify to explicit one when using multiple datasource. Use this or config in application.yml
    // @FlywayDataSource
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DruidDataSource druidDataSource() {
        return new DruidDataSource();
    }

    // @Bean
    // @ConfigurationProperties(prefix = "spring.datasource.kd-pro")
    // public DruidDataSource kdProDataSource() {
    //     return new DruidDataSource();
    // }

    // @Bean
    // @ConfigurationProperties(prefix = "spring.datasource.druid-test")
    // public DruidDataSource druidTestDataSource() {
    //     return new DruidDataSource();
    // }

    // @Bean
    // @ConfigurationProperties(prefix = "spring.datasource.druid-online")
    // public DruidDataSource druidOnlineDataSource() {
    //     return new DruidDataSource();
    // }

    // 初始化数据库操作
    @Bean
    public Boolean initDatabase() {
        // 检查表是否存在
        String checkTableExistsSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'data_source'";
        Integer count = jdbcTemplate.queryForObject(checkTableExistsSql, Integer.class);

        // 如果表不存在，则执行SQL文件
        if (count == null || count == 0) {
            System.out.println("相关表不存在，正在执行sys.sql");

            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
            databasePopulator.addScript(new ClassPathResource("sys.sql"));
            databasePopulator.execute(druidDataSource());
        } else {
            System.out.println("相关表已存在，跳过sys.sql");
        }
        // 返回 true 表示初始化成功
        return true;
    }

    // 从数据库加载数据源
    @Bean
    @DependsOn("initDatabase")
    public Map<String, DruidDataSource> loadDataSource() {
        Map<String, DruidDataSource> map = new HashMap<>();
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
                // 设置数据库连接相关配置
                dataSource.setUrl(url); // 数据库的 URL
                dataSource.setUsername(username); // 数据库的用户名
                dataSource.setPassword(password); // 数据库的密码
                dataSource.setDriverClassName(driver); // 数据库驱动类名

                // 连接池相关配置
                dataSource.setInitialSize(5); // 初始化时建立物理连接的个数
                dataSource.setMaxActive(10); // 最大连接池数量
                dataSource.setMinIdle(2); // 最小连接池数量
                dataSource.setMaxWait(60000); // 获取连接时最大等待时间，单位毫秒

                // 验证连接的 SQL 查询语句
                dataSource.setValidationQuery("SELECT 1 FROM DUAL");

                // 申请连接时执行 validationQuery 检测连接是否有效
                dataSource.setTestOnBorrow(true);

                // 申请连接时执行 validationQuery 检测连接是否有效
                dataSource.setTestWhileIdle(true);

                // 定时关闭空闲连接的时间间隔，连接空闲时间大于等于该值时将被关闭，也是 testWhileIdle 判断的依据之一
                dataSource.setTimeBetweenEvictionRunsMillis(60000);

                // 连接空闲时间超过该值的连接将被断开
                dataSource.setMinEvictableIdleTimeMillis(300000);

                // 配置监控统计拦截的 filters，去掉后监控界面 SQL 无法根据 Session 进行跟踪
                // dataSource.setFilters("stat,wall,log4j");

                // 连接属性配置，可以通过 connectProperties 属性来打开 mergeSql 功能；慢 SQL 记录等
                dataSource.setConnectionProperties("druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000");

                map.put(name, dataSource);
            }
        });

        // TODO 将数据源存入applicationContext
        return map;
    }

}
