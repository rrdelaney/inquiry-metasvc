-- Create youtube metadata schema
CREATE TABLE inq_youtube_metadata (
    id varchar(64) NOT NULL,
    frames bigint NOT NULL,
    duration integer NOT NULL,
    PRIMARY KEY(id)
);

-- Index on video ID for fast querying
CREATE INDEX ON inq_youtube_metadata (id);
