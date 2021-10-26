CREATE TABLE queue
(
    job          TEXT                     NOT NULL,
    consumer     VARCHAR(32)              NOT NULL,
    queued       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp(),
    dequeueAfter TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp(),
    tries        INTEGER                  NOT NULL DEFAULT 0

);

CREATE TABLE queue_error
(
    job      TEXT                     NOT NULL,
    consumer VARCHAR(32)              NOT NULL,
    queued   TIMESTAMP WITH TIME ZONE NOT NULL,
    failedAt TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp(),
    diag     TEXT                     NOT NULL
);

CREATE INDEX queue_take ON queue (consumer, dequeueAfter);
CREATE INDEX queue_error_time ON queue_error (consumer, queued);
CREATE INDEX queue_error_type ON queue_error (consumer, diag);

ALTER TABLE queue
    SET (autovacuum_vacuum_threshold = 25000);
ALTER TABLE queue
    SET (autovacuum_vacuum_scale_factor = 0.00025);
ALTER TABLE queue
    SET (autovacuum_vacuum_cost_limit = 10000);
ALTER TABLE queue
    SET (autovacuum_analyze_threshold = 50000);
ALTER TABLE queue
    SET (autovacuum_analyze_scale_factor = 0.005);
