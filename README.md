# APIJSONBoot

APIJSON7 + SpringBoot3 + JDK17

## 简介

APIJSONBoot 是一个基于 APIJSON7、Spring Boot3 和 JDK 17 构建的项目，旨在简化 API 开发，提供开箱即用的数据处理能力。APIJSON
作为一款零代码、零配置的自动化 API 框架，可以显著提升开发效率，减少重复劳动。结合 Spring Boot 的强大功能和 JDK 17
的新特性，APIJSONBoot 为开发者提供了一个高效、灵活且易于扩展的解决方案。

### 核心特点

1. **零代码、零配置**：通过简单的 JSON 语法即可完成复杂的数据库操作，无需编写 SQL。
2. **自动化 API**：无需手写代码，自动生成增删改查等 API 接口。
3. **多数据源支持**：支持动态配置和管理多个数据源，轻松实现数据隔离和多库操作。
4. **高扩展性**：基于 Spring Boot 构建，支持各种 Spring 生态系统的插件和扩展。
5. **强安全性**：内置多种安全机制，保障数据安全和访问控制。

### 官方链接

- 官网: [APIJSON](http://apijson.cn/)
- GitHub: [APIJSON 仓库](https://github.com/TommyLemon/APIJSON)

## 运行

### 导入 SQL 文件

数据库分为主数据源和其他数据源，主数据源为 `sys`，其他数据源为 `kd_pro_show`。

1. 在配置主数据源前，需先在 `sys` 库导入 `sys.sql` 文件，以创建必要的表和数据。
2. 创建 `kd_pro_show` 库，并在 `kd_pro_show` 库导入 `kd_pro_show.sql` 文件，以创建业务库的表和数据。

### 主数据源配置

主数据源在 `application.yaml` （部署后使用application-prod.yaml）文件中进行配置。

```yaml
# 主数据源的配置文件
mysql:
  # MySQL数据库版本号
  version: 8.3.0
  # MySQL数据库连接URL
  url: jdbc:mysql://192.168.21.125:3306?useSSL=false&serverTimezone=GMT%2B8&useUnicode=true&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
  # MySQL数据库账号
  username: root
  # MySQL数据库密码
  password: root
```

**其中 url username password 需要根据实际情况进行替换**

### 其他数据源配置

其他数据源在 `sys` 库的 `data_source` 表中进行配置。例如：

```sql
INSERT INTO `sys`.`data_source` (`pool_name`, `driver_class_name`, `url`, `username`, `password`)
VALUES ('kd_pro_show', 'com.mysql.cj.jdbc.Driver',
        'jdbc:mysql://192.168.21.191:3306/kd_pro_show?useSSL=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8',
        'root', 'root');
```

**其中 url username password 需要根据实际情况进行替换**

### 运行应用

1. 右键 `DemoApplication` > 选择 `Run As` > `Java Application`

### 打包项目

#### 使用 Fat JAR 打包

```sh
mvn clean package -Pfat-jar
```

#### 使用普通 JAR 和依赖目录打包

```sh
mvn clean package -Pregular-jar
```

### 运行项目

#### 运行 Fat JAR

```sh
java -jar apijson-boot-multi-datasource-7.0.0.jar
```

#### 运行普通 JAR 和依赖目录

```sh
java -cp lib/*:apijson-boot-multi-datasource-7.0.0.jar apijson.APIJSONApplication
```

### 打包与部署

#### 环境依赖

在部署之前，需要使用项目提供的可运行包先搭建好部署环境，具体参考可运行包配置。

#### 部署

1. 使用maven进行打包

2. 将打包生成的apijson-boot-multi-datasource-7.0.0.jar 放至可运行的交付系统部署路径下\work\apijson\中，重启kd-pro-apijson服务即可。
