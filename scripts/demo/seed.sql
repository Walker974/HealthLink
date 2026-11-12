-- HealthLink LOCAL DEMO DATA. Synthetic: invented hospitals, approximate city coordinates, no real people.
-- Never load this into a shared or production database.
-- Safe to run more than once: existing rows are kept (ON CONFLICT DO NOTHING).
-- Explicit IDs stay below 1,000,000; sequences are moved past that at the end so the app never reuses them.
BEGIN;

INSERT INTO regions(id, code, name) VALUES
  (900001, 'RABAT_SALE_KENITRA', 'Rabat-Salé-Kénitra'),
  (900002, 'FES_MEKNES', 'Fès-Meknès'),
  (900003, 'CASABLANCA_SETTAT', 'Casablanca-Settat')
ON CONFLICT DO NOTHING;

INSERT INTO imaging_types(id, code, name) VALUES
  (900011, 'MRI', 'Magnetic resonance imaging'),
  (900012, 'CT', 'Computed tomography'),
  (900013, 'XRAY', 'X-ray imaging'),
  (900014, 'US', 'Ultrasound imaging')
ON CONFLICT DO NOTHING;

-- Regions and imaging types are looked up by code, so rows created earlier through the API are reused.
INSERT INTO hospitals(id, code, name, city, address, region_id, latitude, longitude)
SELECT v.id, v.code, v.name, v.city, v.address, r.id, v.latitude, v.longitude
FROM (VALUES
  (900101, 'DEMO_HAY_RIAD', 'Hay Riad Regional Hospital', 'Rabat', 'Demo address · Hay Riad', 'RABAT_SALE_KENITRA', 33.9590, -6.8670),
  (900102, 'DEMO_AGDAL', 'Agdal Medical Centre', 'Rabat', 'Demo address · Agdal', 'RABAT_SALE_KENITRA', 33.9970, -6.8490),
  (900103, 'DEMO_SALE', 'Salé Al Jadida Hospital', 'Sale', 'Demo address · Salé Al Jadida', 'RABAT_SALE_KENITRA', 34.0330, -6.7600),
  (900201, 'DEMO_SAIS', 'Fès Saïs Regional Hospital', 'Fes', 'Demo address · Saïs', 'FES_MEKNES', 34.0230, -5.0050),
  (900202, 'DEMO_MEKNES', 'Meknès Provincial Hospital', 'Meknes', 'Demo address · Hamria', 'FES_MEKNES', 33.8950, -5.5550),
  (900301, 'DEMO_MAARIF', 'Maarif Regional Hospital', 'Casablanca', 'Demo address · Maarif', 'CASABLANCA_SETTAT', 33.5800, -7.6350),
  (900302, 'DEMO_AIN_SEBAA', 'Ain Sebaa Hospital', 'Casablanca', 'Demo address · Ain Sebaa', 'CASABLANCA_SETTAT', 33.6050, -7.5300)
) AS v(id, code, name, city, address, region_code, latitude, longitude)
JOIN regions r ON r.code = v.region_code
ON CONFLICT DO NOTHING;

INSERT INTO departments(id, code, name, hospital_id) VALUES
  (900401, 'RADIOLOGY', 'Radiology', 900101),
  (900402, 'RADIOLOGY', 'Radiology', 900102),
  (900403, 'RADIOLOGY', 'Radiology', 900103),
  (900404, 'RADIOLOGY', 'Radiology', 900201),
  (900405, 'RADIOLOGY', 'Radiology', 900202),
  (900406, 'RADIOLOGY', 'Radiology', 900301),
  (900407, 'RADIOLOGY', 'Radiology', 900302)
ON CONFLICT DO NOTHING;

-- Staff profiles. keycloak_subject = the fixed user IDs in keycloak-users.json.
INSERT INTO staff_members(id, keycloak_subject, full_name, email, role, hospital_id, department_id) VALUES
  (900001, '5d1c7e2a-0000-4000-8000-000000000001', 'Meryem Amrani', 'manager@healthlink.test', 'HOSPITAL_MANAGER', 900101, NULL),
  (900002, '5d1c7e2a-0000-4000-8000-000000000002', 'Salma Bennani', 'operator@healthlink.test', 'RADIOLOGY_OPERATOR', 900101, 900401),
  (900003, '5d1c7e2a-0000-4000-8000-000000000003', 'Youssef Alaoui', 'technician@healthlink.test', 'MAINTENANCE_TECHNICIAN', 900101, NULL),
  (900004, '5d1c7e2a-0000-4000-8000-000000000004', 'Nour Benali', 'ministry@healthlink.test', 'MINISTRY_OFFICER', NULL, NULL),
  (900005, '5d1c7e2a-0000-4000-8000-000000000005', 'Amine El Idrissi', 'admin@healthlink.test', 'PLATFORM_ADMIN', NULL, NULL),
  -- No login: only reports the Casablanca fault below.
  (900006, NULL, 'Karim Tazi', 'casa.operator@healthlink.test', 'RADIOLOGY_OPERATOR', 900301, 900406)
ON CONFLICT DO NOTHING;

INSERT INTO equipment(id, serial_number, model, status, status_updated_at, department_id, imaging_type_id)
SELECT v.id, v.serial_number, v.model, v.status, v.status_updated_at, v.department_id, t.id
FROM (VALUES
  -- Hay Riad: the hospital the demo accounts work in, with one machine in every repair state.
  (900501, 'DEMO-HR-MRI-1', 'Siemens Magnetom 1.5T', 'AVAILABLE', now() - interval '3 days', 900401, 'MRI'),
  (900502, 'DEMO-HR-MRI-2', 'GE Signa 3T', 'OUT_OF_SERVICE', now() - interval '2 hours', 900401, 'MRI'),
  (900503, 'DEMO-HR-CT-1', 'Philips Incisive CT', 'UNDER_MAINTENANCE', now() - interval '5 hours', 900401, 'CT'),
  (900504, 'DEMO-HR-XR-1', 'Carestream DRX', 'AVAILABLE', now() - interval '6 days', 900401, 'XRAY'),
  (900505, 'DEMO-HR-US-1', 'Mindray Resona', 'UNDER_MAINTENANCE', now() - interval '1 hour', 900401, 'US'),
  -- Agdal
  (900511, 'DEMO-AG-MRI-1', 'Siemens Magnetom 1.5T', 'AVAILABLE', now() - interval '9 days', 900402, 'MRI'),
  (900512, 'DEMO-AG-CT-1', 'Canon Aquilion', 'AVAILABLE', now() - interval '4 days', 900402, 'CT'),
  (900513, 'DEMO-AG-US-1', 'GE Logiq', 'AVAILABLE', now() - interval '2 days', 900402, 'US'),
  -- Salé
  (900521, 'DEMO-SA-XR-1', 'Siemens Ysio', 'AVAILABLE', now() - interval '12 days', 900403, 'XRAY'),
  (900522, 'DEMO-SA-CT-1', 'GE Revolution', 'AVAILABLE', now() - interval '7 days', 900403, 'CT'),
  -- Fès and Meknès
  (900531, 'DEMO-FS-MRI-1', 'Philips Ingenia 1.5T', 'AVAILABLE', now() - interval '10 days', 900404, 'MRI'),
  (900532, 'DEMO-FS-CT-1', 'Siemens Somatom', 'AVAILABLE', now() - interval '3 days', 900404, 'CT'),
  (900533, 'DEMO-FS-XR-1', 'Fujifilm FDR', 'AVAILABLE', now() - interval '8 days', 900404, 'XRAY'),
  (900541, 'DEMO-MK-US-1', 'Samsung RS85', 'AVAILABLE', now() - interval '5 days', 900405, 'US'),
  (900542, 'DEMO-MK-XR-1', 'Carestream DRX', 'AVAILABLE', now() - interval '15 days', 900405, 'XRAY'),
  -- Casablanca
  (900551, 'DEMO-MA-MRI-1', 'GE Signa 1.5T', 'AVAILABLE', now() - interval '6 days', 900406, 'MRI'),
  (900552, 'DEMO-MA-CT-1', 'Philips Incisive CT', 'AVAILABLE', now() - interval '2 days', 900406, 'CT'),
  (900553, 'DEMO-MA-XR-1', 'Siemens Ysio', 'OUT_OF_SERVICE', now() - interval '1 day', 900406, 'XRAY'),
  (900561, 'DEMO-AS-CT-1', 'Canon Aquilion', 'AVAILABLE', now() - interval '11 days', 900407, 'CT'),
  (900562, 'DEMO-AS-US-1', 'Mindray Resona', 'AVAILABLE', now() - interval '3 days', 900407, 'US')
) AS v(id, serial_number, model, status, status_updated_at, department_id, type_code)
JOIN imaging_types t ON t.code = v.type_code
ON CONFLICT DO NOTHING;

-- One repair at each stage, so every role has something to do in the demo.
INSERT INTO work_orders(id, equipment_id, reporter_id, repairer_id, status, description, repair_notes,
                        reported_at, accepted_at, completed_at, open_equipment_id) VALUES
  (900601, 900502, 900002, NULL, 'OPEN', 'Gradient coil error during startup self-test', NULL,
   now() - interval '2 hours', NULL, NULL, 900502),
  (900602, 900503, 900002, 900003, 'IN_PROGRESS', 'Tube overheating warning after three scans', NULL,
   now() - interval '6 hours', now() - interval '5 hours', NULL, 900503),
  (900603, 900505, 900002, 900003, 'AWAITING_VERIFICATION', 'Probe connector loose; image drops out',
   'Replaced probe connector; image quality test passed', now() - interval '1 day', now() - interval '20 hours',
   now() - interval '1 hour', 900505),
  (900604, 900553, 900006, NULL, 'OPEN', 'Detector not responding', NULL,
   now() - interval '1 day', NULL, NULL, 900553)
ON CONFLICT DO NOTHING;

-- History for the Hay Riad repairs (the audit table only accepts inserts).
INSERT INTO audit_records(id, occurred_at, action, equipment_id, hospital_id, work_order_id, actor_subject, actor_staff_id,
                          actor_role, previous_equipment_status, equipment_status, previous_work_order_status, work_order_status, details)
SELECT gen_random_uuid(), v.occurred_at, v.action, v.equipment_id, 900101, v.work_order_id, v.subject, v.staff_id, v.role,
       v.previous_status, v.status, v.previous_order_status, v.order_status, v.details
FROM (VALUES
  (now() - interval '2 hours', 'FAULT_REPORTED', 900502, 900601, '5d1c7e2a-0000-4000-8000-000000000002', 900002, 'RADIOLOGY_OPERATOR',
   'AVAILABLE', 'OUT_OF_SERVICE', NULL, 'OPEN', 'Gradient coil error during startup self-test'),
  (now() - interval '6 hours', 'FAULT_REPORTED', 900503, 900602, '5d1c7e2a-0000-4000-8000-000000000002', 900002, 'RADIOLOGY_OPERATOR',
   'AVAILABLE', 'OUT_OF_SERVICE', NULL, 'OPEN', 'Tube overheating warning after three scans'),
  (now() - interval '5 hours', 'REPAIR_ACCEPTED', 900503, 900602, '5d1c7e2a-0000-4000-8000-000000000003', 900003, 'MAINTENANCE_TECHNICIAN',
   'OUT_OF_SERVICE', 'UNDER_MAINTENANCE', 'OPEN', 'IN_PROGRESS', 'Repair accepted by technician'),
  (now() - interval '1 day', 'FAULT_REPORTED', 900505, 900603, '5d1c7e2a-0000-4000-8000-000000000002', 900002, 'RADIOLOGY_OPERATOR',
   'AVAILABLE', 'OUT_OF_SERVICE', NULL, 'OPEN', 'Probe connector loose; image drops out'),
  (now() - interval '20 hours', 'REPAIR_ACCEPTED', 900505, 900603, '5d1c7e2a-0000-4000-8000-000000000003', 900003, 'MAINTENANCE_TECHNICIAN',
   'OUT_OF_SERVICE', 'UNDER_MAINTENANCE', 'OPEN', 'IN_PROGRESS', 'Repair accepted by technician'),
  (now() - interval '1 hour', 'REPAIR_COMPLETED', 900505, 900603, '5d1c7e2a-0000-4000-8000-000000000003', 900003, 'MAINTENANCE_TECHNICIAN',
   'UNDER_MAINTENANCE', 'UNDER_MAINTENANCE', 'IN_PROGRESS', 'AWAITING_VERIFICATION', 'Replaced probe connector; image quality test passed')
) AS v(occurred_at, action, equipment_id, work_order_id, subject, staff_id, role, previous_status, status,
       previous_order_status, order_status, details)
WHERE NOT EXISTS (SELECT 1 FROM audit_records a WHERE a.work_order_id = v.work_order_id AND a.action = v.action);

-- Keep application-generated IDs clear of the demo IDs.
SELECT setval(seq::regclass, GREATEST(1000000, (SELECT last_value FROM pg_sequences s WHERE s.sequencename = seq AND s.schemaname = current_schema())))
FROM unnest(ARRAY['regions_seq', 'hospitals_seq', 'departments_seq', 'imaging_types_seq',
                  'equipment_seq', 'staff_members_seq', 'work_orders_seq']) AS seq;

COMMIT;
