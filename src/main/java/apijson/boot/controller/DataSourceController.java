package apijson.boot.controller;

import apijson.JSON;
import apijson.boot.config.DataSourceManager;
import apijson.demo.DemoParser;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static apijson.RequestMethod.GETS;

@RestController
@RequestMapping("/datasource")
public class DataSourceController {
    @Autowired
    DataSourceManager dataSourceManager;
    @GetMapping("/list")
    public JSONObject getAllDataSource() {
        String jsonString = """
                {
                     "[]": {
                         "query": 2,
                         "count": 0,
                         "DataSource": {
                             "@column": "pool_name,url"
                         }
                     },
                 }""";
        JSONObject jsonObject = JSON.parseObject(jsonString);
        return new DemoParser(GETS, false).parseResponse(jsonObject);
    }

    @GetMapping("/reload")
    public JSONObject reloadDataSource() {
        JSONObject response = new JSONObject();
        try {
            dataSourceManager.loadDataSource();
            response.put("initDataSource", true);
        } catch (Exception e) {
            response.put("initDataSource", false);
        }
        return response;
    }
}
