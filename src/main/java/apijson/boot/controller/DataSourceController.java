package apijson.boot.controller;

import apijson.JSON;
import apijson.demo.DemoParser;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static apijson.RequestMethod.GETS;

@RestController
@RequestMapping("/datasource")
public class DataSourceController {

    // 内部类 DataSourceEntity
    public static class DataSourceEntity {
        private Long id;
        private String poolName;
        private String username;
        private String password;
        private String url;
        private String driverClassName;

        // 构造函数、Getters 和 Setters
        public DataSourceEntity() {
        }

        public DataSourceEntity(Long id, String poolName, String username, String password, String url, String driverClassName) {
            this.id = id;
            this.poolName = poolName;
            this.username = username;
            this.password = password;
            this.url = url;
            this.driverClassName = driverClassName;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }

    // 模拟一个内存中的数据源列表
    private List<DataSourceEntity> dataSourceList = new ArrayList<>();
    private long nextId = 1;

    @PostMapping
    public String createDataSource(@RequestBody DataSourceEntity dataSource) {
        dataSource.setId(nextId++);
        dataSourceList.add(dataSource);
        return "数据源创建成功";
    }

    @GetMapping("/list")
    public JSONObject getAllDataSource() {
        String jsonString = """
                {
                    "Datasource[]": {
                        "query": 2,
                        "Datasource": {

                        }
                    },
                    "info@": "/Datasource[]/info"
                }""";
        JSONObject jsonObject = JSON.parseObject(jsonString);
        return new DemoParser(GETS, false).parseResponse(jsonObject);
    }

    @GetMapping("/{id}")
    public DataSourceEntity getDataSourceById(@PathVariable Long id) {
        return dataSourceList.stream()
                .filter(ds -> ds.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @PutMapping("/{id}")
    public String updateDataSource(@PathVariable Long id, @RequestBody DataSourceEntity dataSource) {
        DataSourceEntity existingDataSource = getDataSourceById(id);
        if (existingDataSource != null) {
            existingDataSource.setPoolName(dataSource.getPoolName());
            existingDataSource.setUsername(dataSource.getUsername());
            existingDataSource.setPassword(dataSource.getPassword());
            existingDataSource.setUrl(dataSource.getUrl());
            existingDataSource.setDriverClassName(dataSource.getDriverClassName());
            return "数据源更新成功";
        } else {
            return "数据源更新失败，找不到对应的ID";
        }
    }

    @DeleteMapping("/{id}")
    public String deleteDataSource(@PathVariable Long id) {
        DataSourceEntity existingDataSource = getDataSourceById(id);
        if (existingDataSource != null) {
            dataSourceList.remove(existingDataSource);
            return "数据源删除成功";
        } else {
            return "数据源删除失败，找不到对应的ID";
        }
    }
}
