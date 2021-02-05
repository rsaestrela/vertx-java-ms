CREATE TABLE vertxbroker.stock (
    id varchar(36) NOT NULL,
    user_id varchar(16) NOT NULL,
    market varchar(8) NOT NULL,
    price double precision NOT NULL,
    quantity double precision NOT NULL,
    CONSTRAINT VTB_POSITION_PKEY PRIMARY KEY (id)
);