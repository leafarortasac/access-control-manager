FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY shared-contracts/pom.xml ./shared-contracts/
COPY shared-contracts/src ./shared-contracts/src
RUN mvn -f shared-contracts/pom.xml clean install -DskipTests

WORKDIR /app/access-control-manager

COPY access-control-manager/pom.xml .

RUN mvn dependency:go-offline

COPY access-control-manager/src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata
ENV TZ=America/Manaus

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/access-control-manager/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
            "-Xmx512m", \
            "-XX:+UseContainerSupport", \
            "-Duser.timezone=America/Manaus", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]