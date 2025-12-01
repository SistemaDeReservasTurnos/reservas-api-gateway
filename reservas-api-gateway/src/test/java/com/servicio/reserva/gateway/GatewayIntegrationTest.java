package com.servicio.reserva.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.servicio.reserva.gateway.application.dto.requests.LoginRequest;
import com.servicio.reserva.gateway.application.dto.requests.LogoutRequest;
import com.servicio.reserva.gateway.application.dto.requests.RefreshTokenRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private static WireMockServer wireMockServer;
    private static RSAKey rsaKey; // Clave para firmar tokens en los tests

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public WebClient.Builder testWebClientBuilder() {
            return WebClient.builder();
        }
    }

    @BeforeAll
    static void setUp() throws Exception {
        // 1. Iniciar WireMock en un puerto aleatorio
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();

        // 2. Generar Par de Claves RSA para simular el Auth Server
        rsaKey = new RSAKeyGenerator(2048)
                .keyID("test-key-id")
                .generate();

        // 3. Simular el endpoint JWKS del Auth Server (El Gateway lo llamará al inicio)
        String jwksResponse = new JWKSet(rsaKey.toPublicJWK()).toString();

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/oauth2/jwks"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jwksResponse)));
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    // Inyectar el puerto de WireMock en application-test.properties
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("wiremock.server.port", wireMockServer::port);
    }

    // Generador de token JWTs
    private String generateValidJwt(List<String> roles) throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("juan@test.com")
                .issuer("http://localhost:8081")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .claim("roles", roles)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claimsSet
        );

        signedJWT.sign(new RSASSASigner(rsaKey));
        return signedJWT.serialize();
    }

    /**
     * Caso 1: Ruteo Exitoso con Token Válido.
     * <p>
     * Verifica que el Gateway actúe correctamente como un Resource Server y Proxy:
     * 1. Recibe una petición con un JWT válido (firmado con la clave privada de prueba).
     * 2. Valida la firma del token consultando el endpoint JWKS mockeado del Auth Server.
     * 3. Permite el paso de la petición hacia el microservicio de destino (simulado por WireMock).
     * 4. Devuelve la respuesta exacta del microservicio (200 OK) sin alteraciones.
     * <p>
     * Se ejecuta de forma parametrizada para validar todas las rutas críticas del sistema (Usuarios, Servicios, Reservas, etc.).
     */
    @DisplayName("Ruteo Exitoso a Microservicios")
    @ParameterizedTest(name = "Ruta: {0} -> Debe retornar 200 OK")
    @CsvSource(delimiter = '|', textBlock = """
                /api/users/email/juan@test.com | {"id":1,"name":"User","email":"juan@test.com"}
                /api/services/1                | {"id":1,"name":"Corte de Pelo"}
                /api/reservations/1            | {"id":1,"status":"CONFIRMED"}
                /api/payments/1                | {"id":1,"amount":50.0}
                /api/reports/daily             | {"date":"2023-10-27","total":100}
            """)
    void testRoutingSuccess(String path, String mockResponseBody) throws Exception {
        // 1. Stub de WireMock: Simulamos que el microservicio responde correctamente
        wireMockServer.stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponseBody)
                        .withStatus(200)));

        // 2. Generar un token válido (usando el helper)
        String token = generateValidJwt(List.of("ROLE_CLIENTE"));

        // 3. Ejecutar petición al Gateway
        webTestClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(mockResponseBody);
    }

    /**
     * Caso 2: Seguridad - Petición Sin Token.
     * <p>
     * Verifica que la capa de seguridad (SecurityConfig) del Gateway intercepte y rechace
     * inmediatamente cualquier petición a rutas protegidas que no incluya el encabezado Authorization.
     * <p>
     * El Gateway debe devolver 401 Unauthorized sin siquiera intentar contactar al microservicio downstream.
     */
    @DisplayName("Seguridad: Sin Token -> 401 Unauthorized")
    @ParameterizedTest(name = "Ruta: {0}")
    @CsvSource({
            "/api/users/email/juan@test.com",
            "/api/services/1",
            "/api/reservations/1",
            "/api/payments/1",
            "/api/reports/daily"
    })
    void testRoutingUnauthorizedNoToken(String path) {
        webTestClient.get()
                .uri(path)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Caso 3: Seguridad - Token Inválido o Manipulado.
     * <p>
     * Verifica que el Gateway valide criptográficamente la firma del JWT.
     * Se envía un token con estructura válida pero firmado con una clave desconocida o corrupta.
     * <p>
     * El Gateway debe detectar la firma inválida (al no coincidir con la Public Key del JWKS mockeado)
     * y rechazar la petición con 401 Unauthorized.
     */
    @DisplayName("Seguridad: Token Inválido -> 401 Unauthorized")
    @ParameterizedTest(name = "Ruta: {0}")
    @CsvSource({
            "/api/users/email/juan@test.com",
            "/api/services/1",
            "/api/reservations/1",
            "/api/payments/1",
            "/api/reports/daily"
    })
    void testRoutingUnauthorizedInvalidToken(String path) {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.fake.signature";

        webTestClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Caso 4: Flujo BFF - Login Exitoso.
     * <p>
     * Verifica la orquestación del endpoint público de fachada `/api/auth/login`.
     * <p>
     * El Gateway debe:
     * 1. Recibir credenciales simples (email/password) del cliente.
     * 2. Construir una petición segura al Auth Server (inyectando client_id y client_secret en Basic Auth).
     * 3. Solicitar el `grant_type=password`.
     * 4. Devolver exitosamente los tokens (Access y Refresh) obtenidos.
     */
    @Test
    @DisplayName("BFF Login: Orquestación Correcta")
    void testBffLoginSuccess() {
        // 1. Simular respuesta exitosa del Auth Service (/oauth2/token)
        wireMockServer.stubFor(post(urlEqualTo("/oauth2/token"))
                .withHeader("Authorization", containing("Basic")) // Verifica que Gateway envía Basic Auth
                .withRequestBody(containing("grant_type=password")) // Verifica parámetros
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"fake-jwt\", \"refresh_token\":\"fake-refresh\"}")
                        .withStatus(200)));

        // 2. Llamar al Endpoint Público del Gateway
        LoginRequest loginRequest = LoginRequest.builder()
                .email("juan@test.com")
                .password("12345678")
                .build();

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.access_token").isEqualTo("fake-jwt")
                .jsonPath("$.refresh_token").isEqualTo("fake-refresh");
    }

    /**
     * Caso 5: Flujo BFF - Refresco de Token Exitoso.
     * <p>
     * Verifica la orquestación del endpoint de refresco `/api/auth/refresh`.
     * <p>
     * El Gateway debe:
     * 1. Recibir el refresh_token opaco del cliente.
     * 2. Construir una petición segura al Auth Server (inyectando credenciales del cliente).
     * 3. Solicitar el `grant_type=refresh_token`.
     * 4. Devolver los nuevos tokens generados por el Auth Server.
     */
    @Test
    @DisplayName("BFF Refresh: Orquestación Correcta")
    void testBffRefreshSuccess() {
        // 1. Simular respuesta del Auth Service
        wireMockServer.stubFor(post(urlEqualTo("/oauth2/token"))
                .withRequestBody(containing("grant_type=refresh_token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"new-jwt\", \"refresh_token\":\"new-refresh\"}")
                        .withStatus(200)));

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refresh_token("old-refresh-token")
                .build();

        webTestClient.post()
                .uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.access_token").isEqualTo("new-jwt")
                .jsonPath("$.refresh_token").isEqualTo("new-refresh");
    }

    /**
     * Caso 6: Manejo de Errores - Servicio No Disponible.
     * <p>
     * Verifica la lógica de traducción de errores en el `GatewayExceptionHandler`.
     * <p>
     * Escenario:
     * 1. El Auth Server devuelve un error controlado (400 Bad Request) con el código interno
     * `temporarily_unavailable` (simulando caída de base de datos o servicios downstream).
     * 2. El Gateway captura la `WebClientResponseException`.
     * 3. Inspecciona el cuerpo del error.
     * 4. Remapea el estado HTTP a 503 Service Unavailable, informando correctamente al cliente
     * que el problema es temporal y del servidor, no de su petición.
     */
    @Test
    @DisplayName("Manejo de Errores: Auth Service devuelve error temporal -> Gateway devuelve 503")
    void testBffHandleTemporarilyUnavailable() {
        // 1. Simular error 400 con cuerpo específico del Auth Service
        wireMockServer.stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"temporarily_unavailable\", \"error_description\":\"Service Down\"}")
                        .withStatus(400)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("juan@test.com")
                .password("12345678")
                .build();

        // 2. El Gateway debe interceptar ese JSON, ver el código y transformarlo a 503
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.message").isEqualTo("Service Unavailable");
    }

    /**
     * Caso 7: Flujo BFF - Logout Exitoso.
     * <p>
     * Verifica la orquestación del endpoint de cierre de sesión `/api/auth/logout`.
     * <p>
     * El Gateway debe:
     * 1. Recibir el token a revocar.
     * 2. Construir una petición al Auth Server apuntando a `/oauth2/revoke`.
     * 3. Enviar los parámetros necesarios (`token`, `token_type_hint`).
     * 4. Devolver 200 OK al cliente si el Auth Server responde correctamente.
     */
    @Test
    @DisplayName("BFF Logout: Orquestación Correcta con Revocación")
    void testBffLogoutSuccess() {
        // 1. Simular respuesta exitosa del Auth Service en /oauth2/revoke
        wireMockServer.stubFor(post(urlEqualTo("/oauth2/revoke"))
                .withHeader("Authorization", containing("Basic"))
                .withRequestBody(containing("token=some-token"))
                .withRequestBody(containing("token_type_hint=access_token"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // 2. Preparar el request
        LogoutRequest logoutRequest = LogoutRequest.builder()
                .token("some-token")
                .build();

        // 3. Llamar al Endpoint del Gateway
        webTestClient.post()
                .uri("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(logoutRequest)
                .exchange()
                .expectStatus().isOk();
    }
}
