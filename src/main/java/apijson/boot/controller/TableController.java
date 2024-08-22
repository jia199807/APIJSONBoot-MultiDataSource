package apijson.boot.controller;

import apijson.demo.DemoFunctionParser;
import apijson.demo.DemoParser;
import apijson.demo.DemoVerifier;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.rmi.ServerException;
import java.util.Arrays;

import static apijson.RequestMethod.DELETE;
import static apijson.RequestMethod.POST;

@Controller
@RequestMapping("/table")
public class TableController {

    @GetMapping("/delete")
    @ResponseBody
    public JSONObject deleteTable(@RequestParam(name = "alias") String alias) {
        // 动态构建 JSON 对象
        JSONObject accessObject = new JSONObject();
        accessObject.put("alias", alias);

        // 将 Access 对象添加到顶层 JSON 对象中
        JSONObject finalJsonObject = new JSONObject();
        finalJsonObject.put("Access", accessObject);

        // 动态构建 JSON 对象
        JSONObject requestObject = new JSONObject();
        requestObject.put("tag", alias);
        // 将 Request 对象添加到顶层 JSON 对象中
        finalJsonObject.put("Request", requestObject);

        return new DemoParser(DELETE, false).parseResponse(finalJsonObject);
    }

    @GetMapping("/add")
    @ResponseBody
    public JSONObject addTable(@RequestParam("name") String name, @RequestParam(name = "alias", required = false) String alias) {
        // 动态构建 JSON 对象
        JSONObject accessObject = new JSONObject();
        accessObject.put("debug", 0);
        accessObject.put("schema", null);
        accessObject.put("name", name);
        accessObject.put("alias", alias);
        // 通过 JSONArray 生成带有双引号的 JSON 字符串
        accessObject.put("get", new JSONArray().fluentAddAll(Arrays.asList("UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN")).toJSONString());
        accessObject.put("head", new JSONArray().fluentAddAll(Arrays.asList("UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN")).toJSONString());
        accessObject.put("gets", new JSONArray().fluentAddAll(Arrays.asList("LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN")).toJSONString());
        accessObject.put("heads", new JSONArray().fluentAddAll(Arrays.asList("LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN")).toJSONString());
        accessObject.put("post", new JSONArray().fluentAddAll(Arrays.asList("UNKNOWN", "LOGIN", "OWNER", "ADMIN")).toJSONString());
        accessObject.put("put", new JSONArray().fluentAddAll(Arrays.asList("UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN")).toJSONString());
        accessObject.put("delete", new JSONArray().fluentAddAll(Arrays.asList("UNKNOWN", "LOGIN", "CONTACT", "CIRCLE", "OWNER", "ADMIN")).toJSONString());
        accessObject.put("detail", null);
        // 将 Access 对象添加到顶层 JSON 对象中
        JSONObject finalJsonObject = new JSONObject();
        finalJsonObject.put("Access", accessObject);

        // 创建 Request 数组
        JSONArray requestArray = new JSONArray();
        // 定义公共属性
        int debug = 0;
        int version = 1;
        String structure = "{}";
        String detail = "";

        // 创建每个 Request 对象并添加到数组中
        requestArray.add(createRequest(debug, version, "POST", alias, structure, detail));
        requestArray.add(createRequest(debug, version, "GET", alias, structure, detail));
        requestArray.add(createRequest(debug, version, "DELETE", alias, structure, detail));
        requestArray.add(createRequest(debug, version, "PUT", alias, structure, detail));

        // 顶层 JSON 对象添加 Request 数组
        finalJsonObject.put("Request[]", requestArray);

        return new DemoParser(POST, false).parseResponse(finalJsonObject);
    }

    @ResponseBody
    @GetMapping("reload")
    public JSONObject reload() throws ServerException {
        JSONObject jsonObject = new JSONObject();

        // 调用 DemoVerifier.initAccess 并将返回的 JSONObject 放入 jsonObject 中
        JSONObject accessResult = DemoVerifier.initAccess(false, null, null);
        jsonObject.put("initAccess", accessResult.get("ok"));

        // 调用 DemoFunctionParser.init 并将返回的 JSONObject 放入 jsonObject 中
        JSONObject functionParserResult = DemoFunctionParser.init(false, null, null);
        jsonObject.put("initFunctionParser", functionParserResult.get("ok"));

        // 调用 DemoVerifier.initRequest 并将返回的 JSONObject 放入 jsonObject 中
        JSONObject requestResult = DemoVerifier.initRequest(false, null, null);
        jsonObject.put("initRequest", requestResult.get("ok"));

        // 返回包含所有结果的 jsonObject
        return jsonObject;
    }

    private static JSONObject createRequest(int debug, int version, String method, String tag, String structure, String detail) {
        JSONObject request = new JSONObject();
        request.put("debug", debug);
        request.put("version", version);
        request.put("method", method);
        request.put("tag", tag);
        request.put("structure", structure);
        request.put("detail", detail);
        return request;
    }
}
