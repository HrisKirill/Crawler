CREATE TABLE image_info
(
    id              BIGSERIAL PRIMARY KEY,
    original_url    TEXT   NOT NULL,
    file_path       TEXT   NOT NULL,
    original_size   BIGINT NOT NULL,
    compressed_size BIGINT NOT NULL
);