-- Store the stable Google subject so federated sign-in does not depend on a mutable email address.
ALTER TABLE users ADD COLUMN google_subject VARCHAR(255);
CREATE UNIQUE INDEX uq_users_google_subject ON users (google_subject);
