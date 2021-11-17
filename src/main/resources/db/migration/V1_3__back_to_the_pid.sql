DROP INDEX cover_unique_idx;

ALTER TABLE cover DROP COLUMN bibliographicrecordid;
ALTER TABLE cover DROP COLUMN agencyid;
ALTER TABLE cover ADD COLUMN pid varchar not null;

CREATE UNIQUE INDEX cover_unique_idx ON cover (pid);
