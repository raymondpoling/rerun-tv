#
# Dockerfile for file-locator service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/file-locator-0.1.0-SNAPSHOT-standalone.jar file-locator-0.1.0-SNAPSHOT-standalone.jar

ENV DB_USER locator_user
ENV DB_HOST CrystalBall
ENV DB_PORT 3306
ENV DB_PASSWORD $DB_PASSWORD


ENV PORT 4005

EXPOSE 4005

CMD ["java", "-jar", "file-locator-0.1.0-SNAPSHOT-standalone.jar"]
