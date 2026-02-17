package com.foodapp.order_service;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}
	@Test
	void mainMethodRunsSuccessfully() {
		try (MockedStatic<SpringApplication> mockedSpringApp =
					 Mockito.mockStatic(SpringApplication.class)) {

			mockedSpringApp.when(() ->
							SpringApplication.run(OrderServiceApplication.class, new String[]{}))
					.thenReturn(null);

			OrderServiceApplication.main(new String[]{});

			mockedSpringApp.verify(() ->
					SpringApplication.run(OrderServiceApplication.class, new String[]{}));
		}
	}

}
