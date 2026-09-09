CREATE TABLE advertisers (
                             id VARCHAR(36) PRIMARY KEY,
                             name VARCHAR(255) NOT NULL,
                             email VARCHAR(255) NOT NULL,
                             password_hash VARCHAR(255),
                             phone VARCHAR(255),
                             company_name VARCHAR(255),
                             website VARCHAR(255),
                             address1 VARCHAR(255),
                             address2 VARCHAR(255),
                             landmark VARCHAR(255),
                             city VARCHAR(255),
                             state VARCHAR(255),
                             pincode VARCHAR(255),
                             active BOOLEAN NOT NULL,
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                             updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             CONSTRAINT uk_advertisers_name UNIQUE (name),
                             CONSTRAINT uk_advertisers_email UNIQUE (email)
);

CREATE INDEX idx_advertisers_name
    ON advertisers (name);

CREATE INDEX idx_advertisers_email
    ON advertisers (email);