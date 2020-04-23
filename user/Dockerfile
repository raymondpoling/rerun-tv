#
# Dockerfile for playlist-schedule service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>

COPY target/user-0.1.0-SNAPSHOT-standalone.jar user-0.1.0-SNAPSHOT-standalone.jar

ENV DB_USER user_user
ENV DB_HOST CrystalBall
ENV DB_PORT 3306
ENV DB_PASSWORD $DB_PASSWORD


ENV PORT 4002

EXPOSE 4002

CMD ["java", "-jar", "user-0.1.0-SNAPSHOT-standalone.jar"]
