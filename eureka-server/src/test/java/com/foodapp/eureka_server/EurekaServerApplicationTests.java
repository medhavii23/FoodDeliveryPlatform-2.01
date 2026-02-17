package com.foodapp.eureka_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.WebApplicationType;

@SpringBootTest
class EurekaServerApplicationTests {

	@Test
	void contextLoads() {
		// verifies Spring context
	}

	@Test
	void main_shouldRun() {
		// Covers the main() line for IntelliJ coverage
		EurekaServerApplication.main(new String[]{});
	}
}
