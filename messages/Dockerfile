#
# Dockerfile for messages service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/messages-0.1.0-SNAPSHOT-standalone.jar messages-0.1.0-SNAPSHOT-standalone.jar

ENV DB_USER messages_user
ENV DB_HOST CrystalBall
ENV DB_PORT 3306
ENV DB_PASSWORD $DB_PASSWORD


ENV PORT 4010

EXPOSE 4010

CMD ["java", "-jar", "messages-0.1.0-SNAPSHOT-standalone.jar"]
