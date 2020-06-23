#
# Dockerfile for series to playlist script
#

FROM python:3.8.2-buster
MAINTAINER Ray Poling <ruguerv@gmail.com>
ENV REFRESHED_AT Wed Apr 04 2020

RUN pip install requests
RUN pip install redis

ENV REDIS_SERVER $REDIS_SERVER
ENV REDIS_PORT $REDIS_PORT
ENV EXCEPTION_HOST $EXCEPTION_HOST
ENV BASE $BASE
ENV ROOTS $ROOTS
ENV META_HOST $META_HOST
ENV LOCATOR_HOST $LOCATOR_HOST
ENV PLAYLIST_HOST $PLAYLIST_HOST
ENV SCHEDULE_HOST $SCHEDULE_HOST
ENV BUILDER_HOST $BUILDER_HOST

COPY exception_reporting.py exception_reporting.py
COPY root_locations.py root_locations.py
COPY messages.py messages.py
COPY playlist_check.py playlist_check.py
COPY remote_locations.py remote_locations.py
COPY schedule.py schedule.py
COPY series_playlists.py series_playlists.py

CMD ["python", "messages.py"]
