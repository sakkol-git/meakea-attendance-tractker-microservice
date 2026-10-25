CREATE TABLE diagnostic_check (
                                  id BIGSERIAL PRIMARY KEY,
                                  check_name VARCHAR(100) NOT NULL UNIQUE
);