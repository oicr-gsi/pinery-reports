FROM maven:3.6-openjdk-11
WORKDIR /maven-dir
COPY . /maven-dir/
RUN mvn -q clean package

FROM adoptopenjdk:11-jre-hotspot
WORKDIR /app
COPY --from=0 /maven-dir/target/pinery-reports-*-jar-with-dependencies.jar /app/pinery-reports.jar
COPY --from=0 /maven-dir/entrypoint.sh /
RUN mkdir /output && \
    chmod +x /entrypoint.sh

ENV JAR_FILE "pinery-reports.jar"

ENTRYPOINT ["/entrypoint.sh"]

