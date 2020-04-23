#
# Dockerfile for playlist-schedule service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/schedule-0.1.0-SNAPSHOT-standalone.jar schedule-0.1.0-SNAPSHOT-standalone.jar

ENV DB_USER schedule_user
ENV DB_HOST CrystalBall
ENV DB_PORT 3306
ENV DB_PASSWORD $DB_PASSWORD


ENV PORT 4000

EXPOSE 4000

CMD ["java", "-jar", "schedule-0.1.0-SNAPSHOT-standalone.jar"]
