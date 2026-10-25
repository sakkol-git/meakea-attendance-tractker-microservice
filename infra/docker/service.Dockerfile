FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY libs ./libs
COPY services ./services
ARG SERVICE_MODULE
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn --batch-mode --no-transfer-progress -pl services/${SERVICE_MODULE} -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_MODULE
COPY --from=build /workspace/services/${SERVICE_MODULE}/target/${SERVICE_MODULE}-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
