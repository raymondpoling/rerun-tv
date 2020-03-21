#
# Dockerfile for omdb-meta service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/omdb-meta-0.1.0-SNAPSHOT-standalone.jar omdb-meta-0.1.0-SNAPSHOT-standalone.jar

ENV APIKEY $APIKEY
ENV META_HOST $META_HOST
ENV META_PORT $META_PORT
ENV OMDB_HOST $OMDB_HOST
ENV OMDB_PORT $OMDB_PORT


ENV PORT 4011

EXPOSE 4011

CMD ["java", "-jar", "omdb-meta-0.1.0-SNAPSHOT-standalone.jar"]
