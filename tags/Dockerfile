#
# Dockerfile for file-meta service
#

FROM java:openjdk-8-jre
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT 2014-11-25

COPY target/scala-2.13/tags-assembly-0.1.jar tags-assembly-0.1.jar

CMD apt-get install net-tools

ENV NEO4J_USER tags
ENV NEO4J_URI bolt://localhost:7687
ENV NEO4J_PASSWORD $DB_PASSWORD


ENV PORT 4014

EXPOSE 4014

CMD ["java", "-jar", "tags-assembly-0.1.jar"]
