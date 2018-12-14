FROM maven:3.5-jdk-8-alpine
WORKDIR /maven-dir
COPY . /maven-dir/
RUN mvn -q clean package

FROM openjdk:8-jre
WORKDIR /app
RUN mkdir /output
COPY --from=0 /maven-dir/target/pinery-reports-*-jar-with-dependencies.jar /app/pinery-reports.jar
COPY --from=0 /maven-dir/entrypoint.sh /
RUN chmod +x /entrypoint.sh

ENV JAR_FILE "pinery-reports.jar"

ENTRYPOINT ["/entrypoint.sh"]

