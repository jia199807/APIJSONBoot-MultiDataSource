package apijson.boot.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 自定义HttpServletRequestWrapper
class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        InputStream requestInputStream = request.getInputStream();
        this.cachedBody = requestInputStream.readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8));
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final InputStream cachedBodyInputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public int read() throws IOException {
            return this.cachedBodyInputStream.read();
        }

        @Override
        public boolean isFinished() {
            try {
                return cachedBodyInputStream.available() == 0;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // No-op
        }
    }
}

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
    // 使用 LinkedHashMap 记录令牌的添加顺序
    private final LinkedHashMap<String, Long> tokenOrder = new LinkedHashMap<>();
    private static final int MAX_TOKEN_STORE_SIZE = 10000; // 设置令牌存储的最大容量

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

        // 包装请求以缓存请求体
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);

        // 放行规则1: URI中包含 "get" 且请求体中包含 "SysConfig"
        if (cachedRequest.getRequestURI().contains("get") && requestContains(cachedRequest, "SysConfig")) {
            chain.doFilter(cachedRequest, response);
            return;
        }

        // 解析 zzkd 服务的IP地址
        String resolvedIp = resolveContainerIpOrKeepIp(zzkdServiceIp);
        String remoteAddr = httpRequest.getRemoteAddr();

        if (remoteAddr.equals(resolvedIp)) {
            // 如果请求来自 zzkd 服务，直接放行
            chain.doFilter(cachedRequest, response);
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
                chain.doFilter(cachedRequest, response);
                return;
            } else {
                // 如果令牌过期，从内存中删除
                tokenStore.remove(token);
                synchronized (tokenOrder) {
                    tokenOrder.remove(token);
                }
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

            synchronized (tokenStore) {
                // 如果令牌存储的数量已达到上限，则移除最早添加的令牌
                if (tokenStore.size() >= MAX_TOKEN_STORE_SIZE) {
                    String oldestToken = tokenOrder.keySet().iterator().next();
                    tokenStore.remove(oldestToken);
                    tokenOrder.remove(oldestToken);
                }
                // 将新的令牌和它的到期时间添加到令牌存储中
                tokenStore.put(token, expirationDate);
                // 记录令牌的添加顺序
                tokenOrder.put(token, System.currentTimeMillis());
            }

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
        chain.doFilter(cachedRequest, response);
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

    /**
     * 检查请求体中是否包含指定字符串。
     *
     * @param request HttpServletRequest 对象
     * @param keyword 需要检查的字符串
     * @return 如果请求体中包含指定字符串，则返回 true；否则返回 false
     */
    private boolean requestContains(HttpServletRequest request, String keyword) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }
        return requestBody.toString().contains(keyword);
    }
}
