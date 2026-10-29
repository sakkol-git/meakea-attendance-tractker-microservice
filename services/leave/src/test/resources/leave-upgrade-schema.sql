CREATE ROLE leave_migrator LOGIN PASSWORD 'upgrade-only' NOSUPERUSER NOBYPASSRLS;
CREATE ROLE leave_upgrade_runtime LOGIN PASSWORD 'runtime-only' NOSUPERUSER NOBYPASSRLS;
CREATE SCHEMA leave AUTHORIZATION leave_migrator;
CREATE SCHEMA leave_jobs AUTHORIZATION leave_migrator;
-- PostgreSQL checks database CREATE for the existing V2 CREATE SCHEMA IF NOT EXISTS statement.
DO $$ BEGIN EXECUTE format('GRANT CREATE ON DATABASE %I TO leave_migrator',current_database()); END $$;
GRANT USAGE ON SCHEMA leave TO leave_upgrade_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE leave_migrator IN SCHEMA leave
    GRANT SELECT,INSERT,UPDATE,DELETE ON TABLES TO leave_upgrade_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE leave_migrator IN SCHEMA leave
    GRANT USAGE,SELECT ON SEQUENCES TO leave_upgrade_runtime;
