#
# Dockerfile for identity service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/identity-0.1.0-SNAPSHOT-standalone.jar identity-0.1.0-SNAPSHOT-standalone.jar

ENV DB_USER identity_user
ENV DB_HOST CrystalBall
ENV DB_PORT 3306
ENV DB_PASSWORD $DB_PASSWORD


ENV PORT 4012

EXPOSE 4012

CMD ["java", "-jar", "identity-0.1.0-SNAPSHOT-standalone.jar"]
