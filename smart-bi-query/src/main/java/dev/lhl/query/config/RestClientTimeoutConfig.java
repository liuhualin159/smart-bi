package dev.lhl.query.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * 为 RestClient 配置连接/读超时，供 Spring AI 等 HTTP 调用使用，避免大模型请求过早被中断。
 *
 * @author smart-bi
 */
@Configuration
public class RestClientTimeoutConfig
{
    private static final int CONNECT_TIMEOUT_SECONDS = 15;

    @Bean
    public RestClientCustomizer restClientTimeoutCustomizer(SpringAIConfig springAIConfig)
    {
        return builder ->
        {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS));
            factory.setReadTimeout(Duration.ofSeconds(Math.max(30, springAIConfig.getTimeout())));
            builder.requestFactory(factory);
        };
    }
}
