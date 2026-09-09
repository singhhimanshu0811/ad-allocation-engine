CREATE TABLE campaigns (
                           id VARCHAR(36) PRIMARY KEY,

                           advertiser_id VARCHAR(36) NOT NULL,
                           advertiser_email VARCHAR(255) NOT NULL,

                           name VARCHAR(255),
                           description VARCHAR(255),

                           status VARCHAR(255) NOT NULL,

                           creative_file_name VARCHAR(255),
                           creative_url VARCHAR(255),

                           start_instant TIMESTAMP WITH TIME ZONE,
                           end_instant TIMESTAMP WITH TIME ZONE,

                           latitude DOUBLE PRECISION,
                           longitude DOUBLE PRECISION,
                           radius_km DOUBLE PRECISION,

                           budget DOUBLE PRECISION,
                           impressions INTEGER,

                           created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                           updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                           CONSTRAINT fk_campaigns_advertiser
                               FOREIGN KEY (advertiser_id)
                                   REFERENCES advertisers(id)
);

CREATE INDEX idx_campaigns_start_instant
    ON campaigns (start_instant);

CREATE INDEX idx_campaigns_end_instant
    ON campaigns (end_instant);

CREATE INDEX idx_campaigns_status
    ON campaigns (status);