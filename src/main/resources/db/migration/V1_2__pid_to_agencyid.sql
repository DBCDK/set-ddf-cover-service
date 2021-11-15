ALTER TABLE cover
    RENAME COLUMN pid TO agencyid;
DROP INDEX cover_unique_idx;

CREATE UNIQUE INDEX cover_unique_idx ON cover (bibliographicrecordid, agencyid);
