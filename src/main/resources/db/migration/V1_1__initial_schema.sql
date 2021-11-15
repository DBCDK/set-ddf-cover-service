create table cover
(
    id                    serial primary key,
    bibliographicrecordid varchar not null,
    coverexists           boolean not null,
    pid                   varchar,
    modified              timestamp
);

CREATE UNIQUE INDEX cover_unique_idx ON cover (pid);

CREATE OR REPLACE FUNCTION set_modified_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.modified = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER cover_modtime
    BEFORE INSERT OR UPDATE
    ON cover
    FOR EACH ROW
EXECUTE PROCEDURE set_modified_column();
