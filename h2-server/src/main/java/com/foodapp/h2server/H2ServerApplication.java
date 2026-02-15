package com.foodapp.h2server;

import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.sql.SQLException;

@SpringBootApplication
public class H2ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(H2ServerApplication.class, args);
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public Server h2TcpServer() throws SQLException {
		return Server.createTcpServer("-tcpPort", "9092", "-tcpAllowOthers", "-ifNotExists");
	}
}
