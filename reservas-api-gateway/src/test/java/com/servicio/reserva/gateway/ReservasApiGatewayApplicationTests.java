package com.servicio.reserva.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "wiremock.server.port=8081"
})
class ReservasApiGatewayApplicationTests {
    @Test
    void contextLoads() {
    }

}
