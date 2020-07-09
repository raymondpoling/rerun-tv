#
# Dockerfile for deletion service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/deletion-0.1.0-SNAPSHOT-standalone.jar deletion-0.1.0-SNAPSHOT-standalone.jar

ENV DB_USER deletion_user
ENV DB_HOST CrystalBall
ENV DB_PORT 3306
ENV DB_PASSWORD $DB_PASSWORD
ENV PLAYLIST_HOST $PLAYLIST_HOST
ENV SCHEDULE_HOST $SCHEDULE_HOST
ENV META_HOST $META_HOST

ENV PORT 4016

EXPOSE 4016

CMD ["java", "-jar", "deletion-0.1.0-SNAPSHOT-standalone.jar"]
