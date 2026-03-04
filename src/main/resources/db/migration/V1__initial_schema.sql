CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    clerk_user_id VARCHAR(255) NOT NULL UNIQUE,
    role          VARCHAR(50)  NOT NULL DEFAULT 'USER',
    created_date  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_date  TIMESTAMPTZ,
    deleted_date  TIMESTAMPTZ
);

CREATE TABLE urls (
    id              BIGSERIAL    PRIMARY KEY,
    short_url       VARCHAR(10)  NOT NULL UNIQUE,
    destination_url TEXT         NOT NULL,
    user_id         BIGINT,
    password        TEXT,
    is_protected    BOOLEAN      NOT NULL DEFAULT FALSE,
    disabled        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_date    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_date    TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    expiry_date     TIMESTAMPTZ,
    deleted_date    TIMESTAMPTZ,

    CONSTRAINT fk_urls_user
      FOREIGN KEY (user_id)
          REFERENCES users(id)
          ON DELETE SET NULL
);

CREATE INDEX idx_urls_user_active
    ON urls (user_id)
    WHERE deleted_date IS NULL;