#
# Dockerfile for auth service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT {{date}}

COPY auth-{{version}}-standalone.jar auth-{{version}}-standalone.jar

ENV DB_USER auth_user
ENV DB_HOST CrystalBall
ENV DB_PORT 3306
ENV DB_PASSWORD $DB_PASSWORD
ENV SALT $SALT


ENV PORT 4007

EXPOSE 4007

CMD ["java", "-jar", "auth-{{version}}-standalone.jar"]
