FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN chmod +x mvnw && ./mvnw -B -pl backend -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home returnloop
COPY --from=build --chown=returnloop:returnloop /workspace/backend/target/returnloop.jar /app/returnloop.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/returnloop.jar"]
