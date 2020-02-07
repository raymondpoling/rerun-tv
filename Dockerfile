#
# Dockerfile for file-meta service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/file-meta-0.1.0-SNAPSHOT-standalone.jar file-meta-0.1.0-SNAPSHOT-standalone.jar

ENV DB_USER met_user
ENV DB_HOST CrystalBall
ENV DB_PORT 3306
ENV DB_PASSWORD $DB_PASSWORD


ENV PORT 4004

EXPOSE 4004

CMD ["java", "-jar", "file-meta-0.1.0-SNAPSHOT-standalone.jar"]
