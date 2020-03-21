CREATE SCHEMA messages;

CREATE TABLE messages.message (
  message_number SERIAL,
  author VARCHAR(20),
  posted DATETIME,
  title VARCHAR(200),
  information TEXT
) ENGINE INNODB;

CREATE INDEX inverse ON messages.message(message_number DESC);
