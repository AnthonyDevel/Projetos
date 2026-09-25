package com.fluxodados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FluxoDadosApplication {

	public static void main(String[] args) {
		SpringApplication.run(FluxoDadosApplication.class, args);
	}

}
