-- Create video metadata schema
CREATE TABLE inq_video_metadata (
    id varchar(64) NOT NULL,
    frame bigint NOT NULL,
    caption varchar(2048),
    ocr_data varchar(2048),
    image_data varchar(2048),
    PRIMARY KEY(id, frame)
);

-- Index on video ID for fast querying
CREATE INDEX ON inq_video_metadata (id);
