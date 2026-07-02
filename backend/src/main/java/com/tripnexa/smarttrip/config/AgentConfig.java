package com.tripnexa.smarttrip.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AgentConfig {

    /**
     * Dedicated thread pool for agent parallel execution.
     * Avoids polluting ForkJoinPool.commonPool() with potentially slow I/O calls.
     */
    @Bean(name = "agentExecutor")
    Executor agentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("agent-");
        executor.initialize();
        return executor;
    }
}
