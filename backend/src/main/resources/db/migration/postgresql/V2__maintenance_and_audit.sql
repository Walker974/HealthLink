ALTER TABLE regions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE hospitals ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE departments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE imaging_types ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE equipment ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE staff_members ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE SEQUENCE work_orders_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE work_orders (
    id BIGINT PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0, active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6) WITH TIME ZONE, last_modified_date TIMESTAMP(6) WITH TIME ZONE,
    created_by_id BIGINT REFERENCES staff_members(id), last_modified_by_id BIGINT REFERENCES staff_members(id),
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    reporter_id BIGINT NOT NULL REFERENCES staff_members(id),
    repairer_id BIGINT REFERENCES staff_members(id), verifier_id BIGINT REFERENCES staff_members(id),
    status VARCHAR(40) NOT NULL CHECK (status IN ('OPEN', 'IN_PROGRESS', 'AWAITING_VERIFICATION', 'VERIFIED')),
    description VARCHAR(2000) NOT NULL, repair_notes VARCHAR(2000), verification_notes VARCHAR(2000),
    reported_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP(6) WITH TIME ZONE, completed_at TIMESTAMP(6) WITH TIME ZONE,
    reviewed_at TIMESTAMP(6) WITH TIME ZONE, verified_at TIMESTAMP(6) WITH TIME ZONE,
    open_equipment_id BIGINT REFERENCES equipment(id),
    CONSTRAINT uk_work_orders_open_equipment UNIQUE (open_equipment_id),
    CONSTRAINT ck_work_order_open_slot CHECK (
        (status = 'VERIFIED' AND open_equipment_id IS NULL) OR
        (status <> 'VERIFIED' AND open_equipment_id IS NOT NULL AND open_equipment_id = equipment_id)),
    CONSTRAINT ck_work_order_assignment CHECK (status = 'OPEN' OR repairer_id IS NOT NULL),
    CONSTRAINT ck_work_order_verification CHECK (status <> 'VERIFIED' OR
        (verifier_id IS NOT NULL AND verifier_id <> repairer_id AND verified_at IS NOT NULL))
);
CREATE INDEX ix_work_orders_equipment ON work_orders(equipment_id, reported_at, id);

-- Audit identity and resource identifiers are snapshots, not cascading entity relationships.
CREATE TABLE audit_records (
    id UUID PRIMARY KEY, occurred_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    action VARCHAR(40) NOT NULL, equipment_id BIGINT NOT NULL, hospital_id BIGINT NOT NULL,
    work_order_id BIGINT, actor_subject VARCHAR(255) NOT NULL, actor_staff_id BIGINT NOT NULL,
    actor_role VARCHAR(40) NOT NULL,
    previous_equipment_status VARCHAR(30), equipment_status VARCHAR(30) NOT NULL,
    previous_work_order_status VARCHAR(40), work_order_status VARCHAR(40),
    details VARCHAR(2000) NOT NULL
);
CREATE INDEX ix_audit_equipment_time ON audit_records(equipment_id, occurred_at, id);

CREATE FUNCTION reject_audit_record_mutation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Audit records are append-only' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER audit_records_no_update_delete BEFORE UPDATE OR DELETE ON audit_records
    FOR EACH ROW EXECUTE FUNCTION reject_audit_record_mutation();
CREATE TRIGGER audit_records_no_truncate BEFORE TRUNCATE ON audit_records
    FOR EACH STATEMENT EXECUTE FUNCTION reject_audit_record_mutation();
