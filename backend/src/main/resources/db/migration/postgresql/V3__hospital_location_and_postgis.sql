-- Geographic search: hospitals get coordinates; PostGIS computes real distances on the Earth's surface.
-- The PostGIS extension must be available in the database image (postgis/postgis).
CREATE EXTENSION IF NOT EXISTS postgis;

-- Nullable so existing hospitals keep working until an administrator records their location.
ALTER TABLE hospitals ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE hospitals ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE hospitals ADD CONSTRAINT ck_hospitals_coordinates CHECK (
    (latitude IS NULL AND longitude IS NULL) OR
    (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180));

-- Supports public availability searches and Ministry summaries, which filter on status and imaging type.
CREATE INDEX ix_equipment_status_type ON equipment(status, imaging_type_id);
CREATE INDEX ix_work_orders_hospital_queue ON work_orders(status, reported_at, id);
