#
# Dockerfile for auth service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT Wed Apr 01 07:49:47 EDT 2020

COPY target/frontend-0.1.0-SNAPSHOT-standalone.jar frontend-0.1.0-SNAPSHOT-standalone.jar

ENV PORT 4008

EXPOSE 4008

CMD ["java", "-jar", "frontend-0.1.0-SNAPSHOT-standalone.jar"]
