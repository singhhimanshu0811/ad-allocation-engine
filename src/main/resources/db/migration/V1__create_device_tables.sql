create table devices_credential (
    device_id varchar(16) primary key,
    secret_key varchar(256) not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version bigint not null default 0
);

create table devices_routes (
    id bigserial primary key,
    device_id varchar(16) not null references devices_credential(device_id),
    from_location varchar(120) not null,
    to_location varchar(120) not null,
    is_active boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version bigint not null default 0
);

create unique index uq_devices_routes_one_active_per_device
    on devices_routes(device_id)
    where is_active = true;

create unique index uq_devices_routes_device_route
    on devices_routes(device_id, from_location, to_location);

create table devices_tasks (
    device_id varchar(16) primary key,
    poll_interval_seconds integer not null default 60,
    task varchar(20) not null default 'POST_GEO',
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version bigint not null default 0
);

create table geo_locations (
    id bigserial primary key,
    route_id bigint not null references devices_routes(id),
    lat double precision not null,
    lang double precision not null,
    speed double precision,
    heading double precision check (heading >= 0 and heading < 360),
    generated_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version bigint not null default 0
);

create index idx_geo_locations_route_generated_at
    on geo_locations(route_id, generated_at);

create table images (
    id bigserial primary key,
    device_id varchar(16) not null references devices_credential(device_id),
    image_url varchar(2048) not null,
    timestamp timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version bigint not null default 0
);

create index idx_images_device_timestamp
    on images(device_id, timestamp);

create table heartbeats (
    id bigserial primary key,
    device_id varchar(16) not null references devices_credential(device_id),
    heartbeat boolean not null,
    received_at timestamp with time zone not null,
    expires_at timestamp with time zone not null
        default (current_timestamp + interval '7 days'),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version bigint not null default 0
);

create index idx_heartbeats_device_received_at
    on heartbeats(device_id, received_at);
