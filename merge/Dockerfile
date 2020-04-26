#
# Dockerfile for merge service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/merge-0.1.0-SNAPSHOT-standalone.jar merge-0.1.0-SNAPSHOT-standalone.jar

ENV PLAYLIST_HOST $PLAYLIST_HOST
ENV SCHEDULE_HOST $SCHEDULE_HOST
ENV LOCATOR_HOST  $LOCATOR_HOST
ENV META_HOST     $META_HOST
ENV REDIS_URI     $REDIS_URI

ENV PORT 4013

EXPOSE 4013

CMD ["java", "-jar", "merge-0.1.0-SNAPSHOT-standalone.jar"]
