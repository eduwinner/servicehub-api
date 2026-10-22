# =====================================================================
# ServiceHub API — imagem multi-stage (Aula 11, slide 24), Java 21
# Build:  docker build -t servicehub-api .
# =====================================================================

# ---------- Estágio 1: build (JDK + Maven) ----------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Dependências primeiro: esta camada fica em cache enquanto o pom.xml não mudar
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Código-fonte e empacotamento (os testes rodam no CI e no mvnw verify, não aqui:
# precisariam de Docker dentro do Docker para o Testcontainers)
COPY src ./src
RUN mvn -B -q package -DskipTests -Djacoco.skip=true

# ---------- Estágio 2: runtime (só JRE + JAR) ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuário sem privilégios de root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/target/*.jar app.jar

ENV TZ=America/Sao_Paulo
EXPOSE 8080

# O Docker marca o container como unhealthy se o Actuator parar de responder
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Duser.timezone=America/Sao_Paulo", \
  "-jar", "app.jar"]
