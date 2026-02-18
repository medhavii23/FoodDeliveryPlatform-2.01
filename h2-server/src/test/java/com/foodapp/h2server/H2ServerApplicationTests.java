package com.foodapp.h2server;

import org.h2.tools.Server;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class H2ServerApplicationTests {

    // -------- main() coverage --------
    @Test
    void main_shouldCallSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {

            mocked.when(() ->
                            SpringApplication.run(H2ServerApplication.class, new String[]{}))
                    .thenReturn(null);

            H2ServerApplication.main(new String[]{});

            mocked.verify(() ->
                    SpringApplication.run(H2ServerApplication.class, new String[]{}));
        }
    }

    // -------- Bean success --------
    @Test
    void h2TcpServer_shouldCreateServer() throws SQLException {
        H2ServerApplication app = new H2ServerApplication();
        Server server = app.h2TcpServer();
        assertNotNull(server);
        server.stop();
    }

    // -------- Bean exception --------
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
