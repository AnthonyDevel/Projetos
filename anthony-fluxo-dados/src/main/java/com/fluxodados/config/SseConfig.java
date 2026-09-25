package com.fluxodados.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SseConfig {

	@Bean(destroyMethod = "shutdown")
	ScheduledExecutorService sseHeartbeat() {
		return Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r, "sse-heartbeat");
			thread.setDaemon(true);
			return thread;
		});
	}
}
