package com.foodapp.h2server;

import org.h2.tools.Server;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class H2ServerApplicationTests {

    // -------------------------
    // Context loading
    // -------------------------
    @Test
    void contextLoads() {
        // covers @SpringBootApplication
    }

    // -------------------------
    // main() coverage
    // -------------------------
    @Test
    void main_shouldRun_withoutStartingWebServer() {
        SpringApplication app = new SpringApplication(H2ServerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run();
    }

    // -------------------------
    // Bean success branch
    // -------------------------
    @Test
    void h2TcpServer_shouldCreateServer() throws SQLException {
        H2ServerApplication app = new H2ServerApplication();
        Server server = app.h2TcpServer();
        assertNotNull(server);
        server.stop(); // clean shutdown
    }

    // -------------------------
    // Bean exception branch
    // -------------------------
    @Test
    void h2TcpServer_shouldThrowSQLException() {
        try (MockedStatic<Server> mocked = mockStatic(Server.class)) {
            mocked.when(() ->
                    Server.createTcpServer(
                            "-tcpPort", "9092", "-tcpAllowOthers", "-ifNotExists"
                    )
            ).thenThrow(new SQLException("Simulated failure"));

            H2ServerApplication app = new H2ServerApplication();

            assertThrows(SQLException.class, app::h2TcpServer);
        }
    }
}
