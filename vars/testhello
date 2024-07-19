FROM openjdk:17-jdk

WORKDIR /testhello

ADD target/testhello-0.0.1-SNAPSHOT.jar testhello.jar

EXPOSE 8040

ENTRYPOINT ["java", "-jar", "testhello.jar"]
