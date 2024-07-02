# APIJSONBoot

APIJSON + SpringBoot

### 运行

主库数据源在环境变量配置
例如：
-e MYSQL_VERSION=8.3.0 \
-e MYSQL_URL="jdbc:mysql://mysql:3306?useSSL=false&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=UTF-8" \
-e MYSQL_USERNAME=root \
-e MYSQL_PASSWORD=root \

其他数据源在db库，**data_source表配置**
例如：
INSERT INTO `db`.`data_source` (`pool_name`, `driver_class_name`, `url`, `username`, `password`)
VALUES ('kd_pro_show', 'com.mysql.cj.jdbc.Driver', 'jdbc:mysql://192.168.21.191:
3306/kd_pro_show?useSSL=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8', 'root', 'root');

右键 DemoApplication > Run As > Java Application

打包项目
使用 Fat JAR 打包
mvn clean package -Pfat-jar

使用普通 JAR 和依赖目录打包
mvn clean package -Pregular-jar
运行项目
运行 Fat JAR
java -jar apijson-boot-multi-datasource-6.3.0.jar

运行普通 JAR 和依赖目录
java -cp lib/*:apijson-boot-multi-datasource-6.3.0.jar apijson.boot.DemoApplication
