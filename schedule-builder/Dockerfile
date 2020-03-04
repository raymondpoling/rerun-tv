#
# Dockerfile for playlist-schedule service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/schedule-builder-0.1.0-SNAPSHOT-standalone.jar schedule-builder-0.1.0-SNAPSHOT-standalone.jar

ENV PLAYLIST_HOST $PLAYLIST_HOST
ENV PLAYLIST_PORT $PLAYLIST_PORT
ENV SCHEDULE_HOST $SCHEDULE_HOST
ENV SCHEDULE_PORT $SCHEDULE_PORT


ENV PORT 4003

EXPOSE 4003

CMD ["java", "-jar", "schedule-builder-0.1.0-SNAPSHOT-standalone.jar"]
