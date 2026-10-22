# Imagem usada para publicar o sistema (o Render monta ela direto do GitHub, ver render.yaml).
# Etapa 1: compila com Maven + JDK. Etapa 2: roda só o .jar num JRE, sem as ferramentas de compilação.

FROM maven:3.9-eclipse-temurin-21 AS compilacao
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src/ src/
# Os testes rodam antes do push (.\mvnw.cmd test); os de banco precisam de Docker, que não existe aqui dentro.
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
# Usuário sem privilégios: uma falha no sistema não dá acesso de root ao container.
RUN useradd --system --uid 10001 gerenciador
COPY --from=compilacao /app/target/*.jar app.jar
USER gerenciador
# Perfil de produção (application-prod.properties) e datas no horário de Brasília.
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Sao_Paulo
EXPOSE 8080
# Memória ajustada para o plano gratuito (512 MB).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-Xss512k", "-jar", "app.jar"]
