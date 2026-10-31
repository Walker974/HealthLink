-- Original Hibernate-managed schema, before maintenance and version columns.
-- Existing installations baseline at version 1; empty databases execute this file.
CREATE SEQUENCE regions_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE hospitals_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE departments_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE imaging_types_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE equipment_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE staff_members_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE regions (
    id BIGINT PRIMARY KEY, active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6) WITH TIME ZONE, last_modified_date TIMESTAMP(6) WITH TIME ZONE,
    created_by_id BIGINT, last_modified_by_id BIGINT,
    code VARCHAR(30) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL
);
CREATE TABLE hospitals (
    id BIGINT PRIMARY KEY, active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6) WITH TIME ZONE, last_modified_date TIMESTAMP(6) WITH TIME ZONE,
    created_by_id BIGINT, last_modified_by_id BIGINT,
    code VARCHAR(30) NOT NULL UNIQUE, name VARCHAR(160) NOT NULL,
    city VARCHAR(100) NOT NULL, address VARCHAR(250) NOT NULL,
    region_id BIGINT NOT NULL REFERENCES regions(id)
);
CREATE TABLE departments (
    id BIGINT PRIMARY KEY, active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6) WITH TIME ZONE, last_modified_date TIMESTAMP(6) WITH TIME ZONE,
    created_by_id BIGINT, last_modified_by_id BIGINT,
    code VARCHAR(30) NOT NULL, name VARCHAR(120) NOT NULL,
    hospital_id BIGINT NOT NULL REFERENCES hospitals(id),
    CONSTRAINT uk_departments_hospital_code UNIQUE (hospital_id, code)
);
CREATE TABLE imaging_types (
    id BIGINT PRIMARY KEY, active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6) WITH TIME ZONE, last_modified_date TIMESTAMP(6) WITH TIME ZONE,
    created_by_id BIGINT, last_modified_by_id BIGINT,
    code VARCHAR(30) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL
);
CREATE TABLE equipment (
    id BIGINT PRIMARY KEY, active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6) WITH TIME ZONE, last_modified_date TIMESTAMP(6) WITH TIME ZONE,
    created_by_id BIGINT, last_modified_by_id BIGINT,
    serial_number VARCHAR(60) NOT NULL UNIQUE, model VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('AVAILABLE', 'OUT_OF_SERVICE', 'UNDER_MAINTENANCE')),
    status_updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    department_id BIGINT NOT NULL REFERENCES departments(id),
    imaging_type_id BIGINT NOT NULL REFERENCES imaging_types(id)
);
CREATE TABLE staff_members (
    id BIGINT PRIMARY KEY, active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6) WITH TIME ZONE, last_modified_date TIMESTAMP(6) WITH TIME ZONE,
    created_by_id BIGINT REFERENCES staff_members(id), last_modified_by_id BIGINT REFERENCES staff_members(id),
    keycloak_subject VARCHAR(255) UNIQUE, full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    role VARCHAR(40) NOT NULL CHECK (role IN ('RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN',
        'HOSPITAL_MANAGER', 'MINISTRY_OFFICER', 'PLATFORM_ADMIN')),
    hospital_id BIGINT REFERENCES hospitals(id), department_id BIGINT REFERENCES departments(id)
);
ALTER TABLE regions ADD FOREIGN KEY (created_by_id) REFERENCES staff_members(id);
ALTER TABLE regions ADD FOREIGN KEY (last_modified_by_id) REFERENCES staff_members(id);
ALTER TABLE hospitals ADD FOREIGN KEY (created_by_id) REFERENCES staff_members(id);
ALTER TABLE hospitals ADD FOREIGN KEY (last_modified_by_id) REFERENCES staff_members(id);
ALTER TABLE departments ADD FOREIGN KEY (created_by_id) REFERENCES staff_members(id);
ALTER TABLE departments ADD FOREIGN KEY (last_modified_by_id) REFERENCES staff_members(id);
ALTER TABLE imaging_types ADD FOREIGN KEY (created_by_id) REFERENCES staff_members(id);
ALTER TABLE imaging_types ADD FOREIGN KEY (last_modified_by_id) REFERENCES staff_members(id);
ALTER TABLE equipment ADD FOREIGN KEY (created_by_id) REFERENCES staff_members(id);
ALTER TABLE equipment ADD FOREIGN KEY (last_modified_by_id) REFERENCES staff_members(id);
