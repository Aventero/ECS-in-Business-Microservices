CREATE TABLE IF NOT EXISTS user_credentials
(
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles
(
    user_id BIGINT      NOT NULL,
    role    VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_roles_user_credentials
        FOREIGN KEY (user_id)
            REFERENCES user_credentials (id)
            ON DELETE CASCADE
);