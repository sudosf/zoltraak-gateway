# Build stage
FROM amazoncorretto:25-al2023 AS build
WORKDIR /app
RUN dnf install -y tar gzip && dnf clean all
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# Runtime stage
FROM amazoncorretto:25-al2023-headless AS runtime
WORKDIR /app
RUN dnf install -y shadow-utils && dnf clean all
RUN groupadd -r zoltraak && useradd -r -g zoltraak -s /sbin/nologin zoltraak
COPY --from=build /app/target/gateway-*.jar app.jar
RUN chown zoltraak:zoltraak app.jar
USER zoltraak
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
