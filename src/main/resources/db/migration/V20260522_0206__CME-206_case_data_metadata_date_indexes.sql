CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_jur_case_type_created_date
    ON public.case_data USING btree (jurisdiction, case_type_id, created_date);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_jur_case_type_last_modified
    ON public.case_data USING btree (jurisdiction, case_type_id, last_modified)
    WHERE last_modified IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_jur_case_type_last_state_modified_date
    ON public.case_data USING btree (jurisdiction, case_type_id, last_state_modified_date)
    WHERE last_state_modified_date IS NOT NULL;
