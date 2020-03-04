DROP TABLE schedule.schedule;

CREATE TABLE schedule.schedule (
  id SERIAL,
  name VARCHAR(20),
  schedule JSON
);

CREATE UNIQUE INDEX by_name ON schedule.schedule (name);
