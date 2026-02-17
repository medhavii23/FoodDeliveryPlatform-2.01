package com.foodapp.identity_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IdentityServiceApplicationTests {

	@Test
	void contextLoads() {
		// Ensures Spring context loads
	}

	@Test
	void mainMethodRuns() {
		// Explicitly execute main() to cover it
		IdentityServiceApplication.main(new String[] {});
	}
}
