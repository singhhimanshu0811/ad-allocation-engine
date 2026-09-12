CREATE TABLE device_current_state (
                                      device_id VARCHAR(16) PRIMARY KEY,
                                      current_route_id BIGINT NOT NULL,
                                      position geography(Point, 4326) NOT NULL,
                                      distance_along_route DOUBLE PRECISION NOT NULL,
                                      updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                      CONSTRAINT fk_device_current_state_route
                                          FOREIGN KEY (current_route_id)
                                              REFERENCES routes(route_id)
);

CREATE INDEX idx_device_current_state_device_id
    ON device_current_state (device_id);