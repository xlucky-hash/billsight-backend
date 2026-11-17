package com.ldj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean("classifyExecutor")
    public Executor classifyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置十条线程
        executor.setCorePoolSize(10);
        // 备用线程十条
        executor.setMaxPoolSize(20);
        // 阻塞队列
        executor.setQueueCapacity(200);
        // 如果队列满了，由主线程自己跑，防止丢数据
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("classify-task-");
        executor.initialize();
        return executor;
    }

    @Bean("classifyWorkerExecutor")
    public Executor classifyWorkerExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(10);        // = 大模型并发上限
        ex.setMaxPoolSize(20);
        ex.setQueueCapacity(500);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        ex.setThreadNamePrefix("classify-worker-");
        ex.initialize();
        return ex;
    }
}