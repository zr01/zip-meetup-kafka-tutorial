-- Add Payments Staging Table
CREATE TABLE stg_payments
(
  event_id             BIGSERIAL PRIMARY KEY,
  payment_id           TEXT                     NOT NULL,
  account_id           TEXT                     NOT NULL,
  amount               INT                      NOT NULL,
  merchant_id          TEXT                     NOT NULL,
  is_payment_success   BOOL,
  is_notification_sent BOOL,
  event_time           TIMESTAMP WITH TIME ZONE NOT NULL,
  event_source         TEXT                     NOT NULL
);

-- Add index on account_id
CREATE INDEX idx_stg_payments_account_id_event_time
  ON stg_payments
    USING btree (account_id, event_time);

-- Add index on payment_id
CREATE INDEX idx_stg_payments_payment_id
  ON stg_payments
    USING btree (payment_id);

-- Add fct_payments
CREATE TABLE fct_payments
(
  payment_id              TEXT PRIMARY KEY,
  account_id              TEXT                     NOT NULL,
  merchant_id             TEXT                     NOT NULL,
  amount                  INT                      NOT NULL,
  is_payment_success      BOOL,
  is_notification_sent    BOOL,
  payment_processing_time BIGINT,
  payment_completed_at    TIMESTAMP WITH TIME ZONE
);

-- Add index on account_id
CREATE INDEX idx_fct_payments_account_id_completed_at
  ON fct_payments
    USING btree (account_id, payment_completed_at);

-- Add index on payment_id
CREATE INDEX idx_fct_payments_payment_id_completed_at
  ON fct_payments
    USING btree (payment_id, payment_completed_at);