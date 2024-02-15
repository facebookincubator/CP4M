FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /opt/cp4m

COPY pom.xml .
COPY src src

RUN mvn clean -U package -Dcustom.jarName=cp4m -Dmaven.test.skip=true

FROM eclipse-temurin:21
WORKDIR /opt/cp4m
COPY --from=build /opt/cp4m/target/cp4m.jar .

CMD ["java", "-jar", "cp4m.jar"]
