# ETAPA 1: Construcción (Builder)
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiamos el wrapper de Maven y el pom.xml para descargar dependencias
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Damos permisos y descargamos dependencias (modo offline)
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# Copiamos el código fuente de tu Gateway
COPY src ./src

# Compilamos el JAR (saltando tests para agilizar el build en contenedores)
RUN ./mvnw package -DskipTests

# Renombramos el jar generado para facilitar su copia
RUN cp target/*.jar app.jar

# ETAPA 2: Ejecución (Runtime)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Seguridad: Creamos un usuario sin privilegios para correr la app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos ÚNICAMENTE el jar compilado desde la etapa anterior
COPY --from=builder /app/app.jar app.jar

# Exponemos el puerto del API Gateway
EXPOSE 8080

# Punto de entrada
ENTRYPOINT ["java", "-jar", "app.jar"]