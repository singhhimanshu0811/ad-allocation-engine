create table devices_credential (
    device_id varchar(16) primary key,
    secret_key varchar(256) not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version bigint not null default 0
);


create table devices_tasks (
    device_id varchar(16) primary key,
    poll_interval_seconds integer not null default 60,
    task varchar(20) not null default 'POST_GEO',
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version bigint not null default 0
);



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


