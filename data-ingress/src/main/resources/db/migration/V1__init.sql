-- Add Events Staging Table
CREATE TABLE stg_events
(
  event_id           BIGSERIAL PRIMARY KEY,
  event_name         TEXT                     NOT NULL,
  event_ref          TEXT                     NOT NULL,
  unstructured_event JSONB                    NOT NULL,
  event_time         TIMESTAMP WITH TIME ZONE NOT NULL,
  event_source       TEXT                     NOT NULL,
  event_received     TIMESTAMP WITH TIME ZONE NOT NULL
);

-- sequence for primary key
CREATE sequence stg_events_seq increment 1 minvalue 1 start 1;

-- Add index on event ref
CREATE INDEX idx_stg_payment_events_event_ref_event_time
  ON stg_events
    USING btree (event_ref, event_time);

-- Add index on source and time
CREATE INDEX idx_stg_payment_events_event_source_time
  ON stg_events
    USING btree (event_source, event_time);

-- Add fct_payments
CREATE TABLE fct_payments
(
  payment_id              TEXT PRIMARY KEY,
  account_id              TEXT NOT NULL,
  merchant_id             TEXT NOT NULL,
  amount                  INT  NOT NULL,
  payment_success         BOOL,
  notification_sent       BOOL,
  notification_sent_at    TIMESTAMP WITH TIME ZONE,
  payment_started_at      TIMESTAMP WITH TIME ZONE,
  payment_processing_time BIGINT,
  payment_completed_at    TIMESTAMP WITH TIME ZONE,
  updated_at              TIMESTAMP WITH TIME ZONE
);

-- Add index on account_id
CREATE INDEX idx_fct_payments_account_id_completed_at
  ON fct_payments
    USING btree (account_id, payment_completed_at);

-- Add index on payment_id
CREATE INDEX idx_fct_payments_payment_id_completed_at
  ON fct_payments
    USING btree (payment_id, payment_completed_at);