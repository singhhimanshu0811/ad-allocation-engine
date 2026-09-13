ALTER TABLE campaigns
    ADD CONSTRAINT unique_campaign_name UNIQUE (name);

CREATE INDEX idx_campaign_name ON campaigns (name);