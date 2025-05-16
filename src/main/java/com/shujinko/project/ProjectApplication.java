package com.shujinko.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootApplication
public class ProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectApplication.class, args);
	}

	@Bean
	public CommandLineRunner printRoutes(ApplicationContext ctx) {
		return args -> {
			System.out.println("🔍 등록된 컨트롤러 목록:");
			ctx.getBean(RequestMappingHandlerMapping.class)
					.getHandlerMethods()
					.forEach((key, value) -> System.out.println("👉 " + key + " => " + value));
		};
	}
}	