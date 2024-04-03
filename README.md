# APIJSONBoot

APIJSON + SpringBoot 接近成品的 Demo

### 运行

主库数据源在环境变量配置
例如：
-e MYSQL_VERSION=8.3.0 \
-e MYSQL_URL="jdbc:mysql://mysql:3306?useSSL=false&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=UTF-8" \
-e MYSQL_USERNAME=root \
-e MYSQL_PASSWORD=root \

其他数据源在db库，data_source表配置
例如：
INSERT INTO `db`.`data_source` (`pool_name`, `driver_class_name`, `url`, `username`, `password`)
VALUES ('kd_pro_show', 'com.mysql.cj.jdbc.Driver', 'jdbc:mysql://192.168.21.191:
3306/kd_pro_show?useSSL=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8', 'root', 'root');

右键 DemoApplication > Run As > Java Application

### 自定义 API 的说明（非 APIJSON 万能 API）


```
