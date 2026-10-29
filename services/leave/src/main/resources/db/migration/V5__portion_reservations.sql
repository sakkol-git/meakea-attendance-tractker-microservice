ALTER TABLE leave.leave_request ADD CONSTRAINT leave_request_tenant_employee UNIQUE(tenant_id,id,employee_id);
ALTER TABLE leave.leave_day ADD CONSTRAINT leave_day_owner
    FOREIGN KEY(tenant_id,request_id,employee_id) REFERENCES leave.leave_request(tenant_id,id,employee_id);
ALTER TABLE leave.leave_day DROP CONSTRAINT uq_leave_day;
ALTER TABLE leave.leave_day ADD CONSTRAINT leave_request_date UNIQUE(tenant_id,request_id,date);
CREATE TABLE leave.day_reservation (
    tenant_id varchar(64) NOT NULL, employee_id varchar(64) NOT NULL, date date NOT NULL,
    portion varchar(12) NOT NULL CHECK(portion IN ('MORNING','AFTERNOON')), request_id varchar(64) NOT NULL,
    PRIMARY KEY(tenant_id,employee_id,date,portion),
    FOREIGN KEY(tenant_id,request_id,employee_id) REFERENCES leave.leave_request(tenant_id,id,employee_id)
);
-- Flyway's owning role reads its own legacy rows only within this atomic forward migration.
-- Runtime RLS stays enabled; FORCE is restored before commit and no BYPASSRLS grant is introduced.
ALTER TABLE leave.leave_day NO FORCE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_request NO FORCE ROW LEVEL SECURITY;
INSERT INTO leave.day_reservation(tenant_id,employee_id,date,portion,request_id)
SELECT d.tenant_id,d.employee_id,d.date,halves.portion,d.request_id FROM leave.leave_day d
JOIN leave.leave_request r ON r.tenant_id=d.tenant_id AND r.id=d.request_id AND r.employee_id=d.employee_id
CROSS JOIN (VALUES ('MORNING'),('AFTERNOON')) AS halves(portion)
WHERE r.status IN ('PENDING','APPROVED','NEEDS_REVIEW') AND (d.portion='FULL_DAY' OR d.portion=halves.portion);
ALTER TABLE leave.leave_day FORCE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_request FORCE ROW LEVEL SECURITY;
ALTER TABLE leave.day_reservation ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.day_reservation FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_reservation_tenant ON leave.day_reservation
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));

CREATE FUNCTION leave.reserve_day_portions() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
    IF NEW.portion NOT IN ('FULL_DAY','MORNING','AFTERNOON') THEN
        RAISE EXCEPTION 'Invalid leave portion' USING ERRCODE='23514';
    END IF;
    INSERT INTO leave.day_reservation(tenant_id,employee_id,date,portion,request_id)
    SELECT NEW.tenant_id,NEW.employee_id,NEW.date,halves.portion,NEW.request_id
    FROM (VALUES ('MORNING'),('AFTERNOON')) AS halves(portion)
    WHERE NEW.portion='FULL_DAY' OR NEW.portion=halves.portion;
    RETURN NEW;
END;
$$;
CREATE TRIGGER leave_day_reserve AFTER INSERT ON leave.leave_day
    FOR EACH ROW EXECUTE FUNCTION leave.reserve_day_portions();
CREATE FUNCTION leave.release_day_portions() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
    DELETE FROM leave.day_reservation WHERE tenant_id=OLD.tenant_id AND employee_id=OLD.employee_id
        AND date=OLD.date AND request_id=OLD.request_id;
    RETURN OLD;
END;
$$;
CREATE TRIGGER leave_day_release AFTER DELETE ON leave.leave_day
    FOR EACH ROW EXECUTE FUNCTION leave.release_day_portions();
CREATE FUNCTION leave.reject_reservation_rewrite() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Leave day reservations cannot be rewritten; cancel and submit explicitly';
END;
$$;
CREATE TRIGGER leave_day_no_rewrite BEFORE UPDATE ON leave.leave_day
    FOR EACH ROW EXECUTE FUNCTION leave.reject_reservation_rewrite();
