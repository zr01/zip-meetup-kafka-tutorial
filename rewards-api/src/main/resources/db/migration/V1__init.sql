-- Add Users Table
CREATE TABLE users
(
  id           TEXT PRIMARY KEY,
  email        TEXT                     NOT NULL,
  first_name   TEXT                     not null,
  last_name    TEXT                     not null,
  status       TEXT                     NOT NULL,
  display_name TEXT,
  profile      JSONB,
  created_on   TIMESTAMP WITH TIME ZONE NOT NULL default now(),
  created_by   TEXT                     NOT NULL,
  modified_on  TIMESTAMP WITH TIME ZONE,
  modified_by  TEXT
);
