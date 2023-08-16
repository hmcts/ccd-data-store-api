package uk.gov.hmcts.ccd.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor(@Value("${async.executor.core.pool.size}") Integer corePoolSize,
                                  @Value("${async.executor.max.pool.size}") Integer maxPoolSize,
                                  @Value("${async.executor.queue.capacity}") Integer queueCapacity,
                                  @Value("${async.executor.prefix}") String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(prefix);
        executor.setTaskDecorator(new ContextPreservingDecorator());
        return executor;
    }

    private static class ContextPreservingDecorator implements TaskDecorator {

        @NonNull
        @Override
        public Runnable decorate(@NonNull Runnable originalTask) {
            RequestAttributes context = RequestContextHolder.currentRequestAttributes();
            SecurityContext securityContext = SecurityContextHolder.getContext();

            return () -> {
                try {
                    RequestContextHolder.setRequestAttributes(context);
                    SecurityContextHolder.setContext(securityContext);
                    originalTask.run();
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                    SecurityContextHolder.clearContext();
                }
            };
        }
    }
}
