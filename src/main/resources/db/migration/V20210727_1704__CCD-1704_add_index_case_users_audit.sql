CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_users_audit_case_data_id_user_id_case_role ON case_users_audit USING btree (case_data_id, user_id, case_role);
