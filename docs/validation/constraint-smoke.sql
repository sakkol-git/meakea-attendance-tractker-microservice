-- Documentation DDL smoke checks only. Run against a disposable database after exported schema.sql.
-- This does not validate application authorization or concurrent transaction behavior.
BEGIN;
INSERT INTO identity_org.tenant (id,name,slug,timezone,country,status)
VALUES ('00000000-0000-4000-8000-000000000001','Tenant A','doc-a','Asia/Phnom_Penh','KH','ACTIVE'),
       ('00000000-0000-4000-8000-000000000002','Tenant B','doc-b','Asia/Phnom_Penh','KH','ACTIVE');
INSERT INTO attendance.employee_ref (tenant_id,employee_id,full_name,status,hire_date,source_version,source_event_id)
VALUES ('00000000-0000-4000-8000-000000000001','00000000-0000-4000-8000-000000000010','Employee','ACTIVE','2026-10-01',1,'00000000-0000-4000-8000-000000000020');
INSERT INTO attendance.attendance_session
(tenant_id,id,employee_id,work_date,check_in_at,source,confirm_status,shift_snapshot)
VALUES ('00000000-0000-4000-8000-000000000001','00000000-0000-4000-8000-000000000030','00000000-0000-4000-8000-000000000010','2026-10-22','2026-10-22T01:00:00Z','TAP','NONE','{}');
DO $$
BEGIN
  BEGIN
    INSERT INTO attendance.attendance_session
      (tenant_id,id,employee_id,work_date,check_in_at,source,confirm_status,shift_snapshot)
    VALUES ('00000000-0000-4000-8000-000000000002','00000000-0000-4000-8000-000000000031','00000000-0000-4000-8000-000000000010','2026-10-22','2026-10-22T01:00:00Z','TAP','NONE','{}');
    RAISE EXCEPTION 'Cross-tenant employee FK was not enforced';
  EXCEPTION WHEN foreign_key_violation THEN NULL;
  END;
  BEGIN
    INSERT INTO attendance.attendance_session
      (tenant_id,id,employee_id,work_date,check_in_at,source,confirm_status,shift_snapshot)
    VALUES ('00000000-0000-4000-8000-000000000001','00000000-0000-4000-8000-000000000032','00000000-0000-4000-8000-000000000010','2026-10-22','2026-10-22T01:01:00Z','TAP','NONE','{}');
    RAISE EXCEPTION 'Second open session was not rejected';
  EXCEPTION WHEN unique_violation THEN NULL;
  END;
  BEGIN
    UPDATE attendance.attendance_session SET check_out_at='2026-10-21T01:00:00Z'
    WHERE tenant_id='00000000-0000-4000-8000-000000000001' AND id='00000000-0000-4000-8000-000000000030';
    RAISE EXCEPTION 'Negative session interval was not rejected';
  EXCEPTION WHEN check_violation THEN NULL;
  END;
END $$;
INSERT INTO leave.employee_ref (tenant_id,employee_id,full_name,status,hire_date,source_version,source_event_id)
VALUES ('00000000-0000-4000-8000-000000000001','00000000-0000-4000-8000-000000000010','Employee','ACTIVE','2026-10-01',1,'00000000-0000-4000-8000-000000000020');
INSERT INTO leave.leave_type (tenant_id,id,code,name,paid,requires_attachment,max_days_per_request,allow_half_day)
VALUES ('00000000-0000-4000-8000-000000000001','00000000-0000-4000-8000-000000000040','ANNUAL','Annual',true,false,20,true);
DO $$
BEGIN
  BEGIN
    INSERT INTO leave.leave_balance
      (tenant_id,employee_id,leave_type_id,year,entitled,carried_over,used,pending)
    VALUES ('00000000-0000-4000-8000-000000000001','00000000-0000-4000-8000-000000000010','00000000-0000-4000-8000-000000000040',2026,1,0,1,1);
    RAISE EXCEPTION 'Negative leave availability was not rejected';
  EXCEPTION WHEN check_violation THEN NULL;
  END;
END $$;
INSERT INTO attendance.inbox
(tenant_id,consumer_name,event_id,topic,partition,source_offset,aggregate_id,aggregate_version)
VALUES ('00000000-0000-4000-8000-000000000001','doc-test','00000000-0000-4000-8000-000000000050','org.employee.v1',0,1,'employee',1);
DO $$
BEGIN
  BEGIN
    INSERT INTO attendance.inbox
      (tenant_id,consumer_name,event_id,topic,partition,source_offset,aggregate_id,aggregate_version)
    VALUES ('00000000-0000-4000-8000-000000000001','doc-test','00000000-0000-4000-8000-000000000050','org.employee.v1',0,2,'employee',1);
    RAISE EXCEPTION 'Duplicate inbox event was not rejected';
  EXCEPTION WHEN unique_violation THEN NULL;
  END;
END $$;
ROLLBACK;
