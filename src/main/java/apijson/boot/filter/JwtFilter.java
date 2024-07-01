package apijson.boot.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT过滤器，用于令牌验证和授权。
 */
@Component
@WebFilter(urlPatterns = "/*")
public class JwtFilter implements Filter {


    private final RestTemplate restTemplate;

    @Value("${zzkd.service.validate-token-filter-enabled:true}")
    private boolean validateTokenFilterEnabled;

    @Value("${zzkd.service.ip}")
    private String zzkdServiceIp;

    @Value("${zzkd.service.port}")
    private int zzkdServicePort;

    // 存储有效令牌及其到期时间
    private final ConcurrentHashMap<String, Date> tokenStore = new ConcurrentHashMap<>();

    public JwtFilter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化操作，如果需要的话
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!validateTokenFilterEnabled) {
            // 如果令牌验证过滤器未启用，直接放行
            chain.doFilter(request, response);
            return;
        }

        // 解析 zzkd 服务的IP地址
        String resolvedIp = resolveContainerIpOrKeepIp(zzkdServiceIp);
        String remoteAddr = httpRequest.getRemoteAddr();

        if (remoteAddr.equals(resolvedIp)) {
            // 如果请求来自 zzkd 服务本身，直接放行
            System.out.println("请求来自 zzkd 服务本身，放行请求");
            chain.doFilter(request, response);
            return;
        }


        // 从请求头中获取 Authorization 头
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            // 如果没有提供 Authorization 头或者格式不正确，返回 401 错误
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpResponse.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        // 去掉 "Bearer " 前缀，只保留令牌部分
        token = token.substring(7);

        // 检查令牌是否在内存中，并且没有过期
        if (tokenStore.containsKey(token)) {
            Date expirationDate = tokenStore.get(token);
            if (expirationDate.after(new Date())) {
                // 如果令牌有效，继续处理请求
                chain.doFilter(request, response);
                return;
            } else {
                // 如果令牌过期，从内存中删除
                tokenStore.remove(token);
            }
        }

        // 准备调用 zzkd 服务的验证接口的 URL
        String zzkdServiceUrl = "http://" + zzkdServiceIp + ":" + zzkdServicePort + "/api/v1/auth/validate";

        try {
            // 准备 JSON 数据
            Map<String, String> jsonData = Map.of("token", token);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(jsonData, headers);

            // 调用 zzkd 服务的验证接口
            ResponseEntity<Map> zzkdServiceResponse = restTemplate.exchange(
                    zzkdServiceUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            Map<String, Object> responseBody = zzkdServiceResponse.getBody();
            // 如果响应为空，返回 500 错误
            if (responseBody == null) {
                httpResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                httpResponse.getWriter().write("Failed to validate token: response is null");
                return;
            }

            // 如果令牌无效，返回 401 错误
            if (!Boolean.TRUE.equals(responseBody.get("isValid"))) {
                httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
                httpResponse.getWriter().write("Invalid token");
                return;
            }

            // 获取令牌的到期时间并保存到内存中
            String expirationStr = (String) responseBody.get("expiration");
            Date expirationDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(expirationStr);
            tokenStore.put(token, expirationDate);

        } catch (HttpClientErrorException e) {
            // 捕获 HTTP 客户端错误，返回 401 错误
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpResponse.getWriter().write("Invalid token");
            return;
        } catch (Exception e) {
            // 捕获其他错误，返回 500 错误
            httpResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            httpResponse.getWriter().write("An error occurred while validating token");
            return;
        }

        // 如果令牌有效，继续处理请求
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 销毁操作，如果需要的话
    }

    // 定时任务，每小时清理过期令牌
    @Scheduled(fixedRate = 3600000)
    public void cleanExpiredTokens() {
        Date now = new Date();
        tokenStore.entrySet().removeIf(entry -> entry.getValue().before(now));
    }

    /**
     * 解析容器名或者直接使用IP地址。
     *
     * @param ipOrContainerName IP地址或者容器名
     * @return 解析后的IP地址
     */
    private String resolveContainerIpOrKeepIp(String ipOrContainerName) {
        try {
            InetAddress address = InetAddress.getByName(ipOrContainerName);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("解析容器 IP 失败：" + ipOrContainerName, e);
        }
    }
}
