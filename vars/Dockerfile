FROM openjdk:17-jdk

WORKDIR /java-app

ADD target/java-app-0.0.1-SNAPSHOT.jar java-app.jar

EXPOSE 8060

ENTRYPOINT ["java", "-jar", "java-app.jar"]
