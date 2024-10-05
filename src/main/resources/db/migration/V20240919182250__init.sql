CREATE TABLE image_info
(
    id           BIGSERIAL PRIMARY KEY,
    original_url TEXT NOT NULL,
    file_path    TEXT NOT NULL
);