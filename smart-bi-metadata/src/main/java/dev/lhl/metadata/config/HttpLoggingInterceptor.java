package dev.lhl.metadata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

/**
 * HTTP请求日志拦截器
 * 用于记录Spring AI的HTTP请求URL，便于调试
 * 
 * @author smart-bi
 */
public class HttpLoggingInterceptor implements ClientHttpRequestInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
    
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException
    {
        URI uri = request.getURI();
        log.info("HTTP请求: {} {}", request.getMethod(), uri);
        log.debug("HTTP请求头: {}", request.getHeaders());
        
        try
        {
            ClientHttpResponse response = execution.execute(request, body);
            log.info("HTTP响应: {} {} - Status: {}", request.getMethod(), uri, response.getStatusCode());
            return response;
        }
        catch (IOException e)
        {
            log.error("HTTP请求失败: {} {} - Error: {}", request.getMethod(), uri, e.getMessage());
            throw e;
        }
    }
}
