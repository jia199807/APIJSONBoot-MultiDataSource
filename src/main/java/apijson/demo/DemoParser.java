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

package apijson.demo;

import apijson.RequestMethod;
import apijson.StringUtil;
import apijson.boot.controller.DemoController;
import apijson.demo.model.Privacy;
import apijson.framework.APIJSONObjectParser;
import apijson.framework.APIJSONParser;
import apijson.orm.SQLConfig;
import com.alibaba.fastjson.JSONObject;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


/**
 * 请求解析器
 * 具体见 https://github.com/Tencent/APIJSON/issues/38
 *
 * @author Lemon
 */
public class DemoParser extends APIJSONParser<Long> {

    Map<String, Object> yamlData;

    {
        try {
            yamlData = System.getenv("CONFIG_LOCATION") == null ?
                    new Yaml().load(DemoSQLConfig.class.getClassLoader().getResourceAsStream("application.yaml")) :
                    new Yaml().load(Files.newInputStream(Paths.get(System.getenv("CONFIG_LOCATION"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




    public static final Map<String, HttpSession> KEY_MAP;

    static {
        KEY_MAP = new HashMap<>();
    }

    public DemoParser() {
        super();
        setNeedVerifyContent(false);
    }

    public DemoParser(RequestMethod method) {
        super(method);
    }

    public DemoParser(RequestMethod method, boolean needVerify) {
        super(method, needVerify);
    }

    // 最大嵌套层级
    public int MAX_QUERY_DEPTH = 20;

    @Override
    public int getMaxQueryDepth() {
        return MAX_QUERY_DEPTH;
    }

    // 最大查询数量
    private int maxQueryCount = (int) yamlData.get("maxQueryCount");
    // 最大更新数量
    private int maxUpdateCount = (int) yamlData.get("maxUpdateCount");
    // 最大分页数量
    private int maxQueryPage = (int) yamlData.get("maxQueryPage");

    // // 最大查询数量
    // private int maxQueryCount = 2000;
    // // 最大更新数量
    // private int maxUpdateCount = 2000;
    // // 最大分页数量
    // private int maxQueryPage = 2000;

    //	可重写来设置最大查询数量
    @Override
    public int getMaxQueryCount() {
        return maxQueryCount;
    }
    @Override
    public int getMaxUpdateCount() {
        return maxUpdateCount;
    }

    @Override
    public int getMaxQueryPage() {
        return maxQueryPage;
    }

    @Override
    public int getMaxObjectCount() {
        return getMaxUpdateCount();
    }

    @Override
    public int getMaxSQLCount() {
        return getMaxUpdateCount();
    }


    @Override
    public JSONObject parseResponse(JSONObject request) {
        try {  // 内部使用，可通过这种方式来突破限制，获得更大的自由度
            HttpSession session = KEY_MAP.get(request.getString("key"));
            // DemoVerifier.verifyLogin(session);
            if (session != null) {
                request.remove("key");
                maxQueryCount = 1000;
            }
        } catch (Exception e) {
        }

        return super.parseResponse(request);
    }

    @Override
    public APIJSONObjectParser<Long> createObjectParser(JSONObject request, String parentPath, SQLConfig<Long> arrayConfig
            , boolean isSubQuery, boolean isTable, boolean isArrayMainTable) throws Exception {
        return new DemoObjectParser(getSession(), request, parentPath, arrayConfig
                , isSubQuery, isTable, isArrayMainTable).setMethod(getMethod()).setParser(this);
    }

    // 实现应用层与数据库共用账号密码，可用于多租户、SQLAuto 等 <<<<<<<<<<<<<<<<
    private boolean asDBAccount;
    private String dbAccount;
    private String dbPassword;

    @Override
    public APIJSONParser<Long> setSession(HttpSession session) {
        Boolean asDBAccount = (Boolean) session.getAttribute(DemoController.AS_DB_ACCOUNT);
        this.asDBAccount = asDBAccount != null && asDBAccount;
        if (this.asDBAccount) {
            // User user = (User) session.getAttribute(DemoController.USER_);
            // this.dbAccount = user.getName();
            Privacy privacy = (Privacy) session.getAttribute(DemoController.PRIVACY_);
            this.dbAccount = privacy.getPhone();
            this.dbPassword = privacy.get__password();
        }

        return super.setSession(session);
    }

    @Override
    public JSONObject executeSQL(SQLConfig<Long> config, boolean isSubquery) throws Exception {
        if (asDBAccount && config instanceof DemoSQLConfig) {
            DemoSQLConfig cfg = (DemoSQLConfig) config;
            if (StringUtil.isEmpty(cfg.getDBAccount())) {
                cfg.setDBAccount(dbAccount);
            }
            if (StringUtil.isEmpty(cfg.getDBPassword())) {
                cfg.setDBPassword(dbPassword);
            }
        }
        return super.executeSQL(config, isSubquery);
    }

    // 实现应用层与数据库共用账号密码，可用于多租户、SQLAuto 等 >>>>>>>>>>>>>>>

}
