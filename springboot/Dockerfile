FROM docker.io/openjdk:20
USER root

RUN useradd -u 1001 appuser \
    && mkdir /java-app \
    && chown appuser /java-app

ARG app
COPY /target/*.jar /java-app/app.jar
RUN chmod -R ug+xrw /java-app
USER appuser
EXPOSE 8081 9090
WORKDIR /java-app


ENTRYPOINT ["java", "-jar", "app.jar"]
