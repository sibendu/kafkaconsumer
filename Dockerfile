FROM openjdk:11
ARG JAR_FILE=build/libs/kafkaconsumer-1.0.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]