#
# Dockerfile for format service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/format-0.1.0-SNAPSHOT-standalone.jar format-0.1.0-SNAPSHOT-standalone.jar

ENV USER_HOST     $USER_HOST
ENV MERGE_HOST	  $MERGE_HOST

ENV PORT 4009

EXPOSE 4009

CMD ["java", "-jar", "format-0.1.0-SNAPSHOT-standalone.jar"]
