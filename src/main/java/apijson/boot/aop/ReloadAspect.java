package apijson.boot.aop;

import apijson.boot.controller.TableController;
import com.alibaba.fastjson.JSONObject;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ReloadAspect {

    @Autowired
    private TableController tableController;

    // 拦截 addTable 和 deleteTable 方法的执行
    @AfterReturning(pointcut = "execution(* apijson.boot.controller.TableController.addTable(..)) || execution(* apijson.boot.controller.TableController.deleteTable(..))", returning = "result")
    public void afterTableOperation(Object result) throws Throwable {
        invokeReload((JSONObject) result);
    }

    private void invokeReload(JSONObject res) throws Throwable {
        // 调用 reload 方法，并将结果合并到原始返回的 JSON 对象中
        JSONObject reloadResult = tableController.reloadTable();
        res.put("reload", reloadResult);
    }
}
