CREATE SCHEMA schedule;

CREATE TABLE schedule.schedule (
  id SERIAL,
  name VARCHAR(20),
  schedule LONGTEXT
);

CREATE UNIQUE INDEX by_name ON schedule.schedule (name);
