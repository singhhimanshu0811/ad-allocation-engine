ALTER TABLE devices_routes
    ADD CONSTRAINT uq_devices_routes_route_id UNIQUE (route_id);

CREATE INDEX idx_route_stops_perf
    ON route_stops (route_id, stop_sequence_number ASC);