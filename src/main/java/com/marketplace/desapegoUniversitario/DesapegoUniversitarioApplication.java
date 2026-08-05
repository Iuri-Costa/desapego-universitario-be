package com.marketplace.desapegoUniversitario;

import me.paulschwarz.springdotenv.spring.DotenvApplicationInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class DesapegoUniversitarioApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(DesapegoUniversitarioApplication.class)
			.initializers(new DotenvApplicationInitializer())
			.run(args);
	}
}
