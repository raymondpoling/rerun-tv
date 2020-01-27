#
# Dockerfile for playlist-schedule service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/playlist-playlist-0.1.0-standalone.jar playlist-playlist-0.1.0-standalone.jar

ENV DB_USER playlist_user
ENV DB_HOST CrystalBall
ENV DB_PORT 3306
ENV DB_PASSWORD $DB_PASSWORD


ENV PORT 4001

EXPOSE 4001

CMD ["java", "-jar", "playlist-playlist-0.1.0-standalone.jar"]
