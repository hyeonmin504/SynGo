package backend.synGo.config.sse;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements WebMvcConfigurer, AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        // @Async 용
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);    // 기본 스레드 수
        executor.setMaxPoolSize(100);    // 최대 스레드 수
        executor.setQueueCapacity(500);  // 큐 용량
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // WebMvc 비동기 처리 용
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(100);
        taskExecutor.setQueueCapacity(500);
        taskExecutor.setThreadNamePrefix("mvc-async-");
        taskExecutor.initialize();

        configurer.setTaskExecutor(taskExecutor);
        configurer.setDefaultTimeout(30_000); // 타임아웃 설정 (ms)
    }
}