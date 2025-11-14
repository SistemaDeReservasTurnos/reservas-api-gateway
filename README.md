# API Gateway (Sistema de Reservas)

Este proyecto es el único punto de entrada (Single Point of Entry) para el ecosistema de microservicios del sistema de reservas y turnos. Está construido con Spring Cloud Gateway y es responsable de la seguridad central, el enrutamiento y la abstracción de la lógica de autenticación.

## Responsabilidades Principales

1. **Enrutamiento (Routing):** Actúa como un proxy inverso, redirigiendo las peticiones entrantes (ej. /api/reservas/**) al microservicio interno correspondiente (ej. reservas-agenda-service).

2. **Validación de Autenticación (JWT):** Intercepta todas las peticiones a endpoints protegidos. Valida el access_token (JWT) contactando el endpoint JWKS (/oauth2/jwks) del reservas-auth-service para verificar la firma RSA. Si el token es inválido o ha caducado, rechaza la petición (401 Unauthorized).

3. **Fachada de Autenticación Segura (BFF):** Abstrae la complejidad de OAuth2.0 de los clientes (web/móvil). Expone endpoints simples (ej. /api/auth/login, /api/auth/refresh) y orquesta internamente las llamadas seguras (con clientId/clientSecret) a los endpoints /oauth2/token del reservas-auth-service.
