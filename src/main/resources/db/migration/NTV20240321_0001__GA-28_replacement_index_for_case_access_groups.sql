DROP INDEX IF EXISTS idx_case_data_case_access_groups;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_case_access_groups ON public.case_data USING btree (btrim(upper((data #>> '{CaseAccessGroups}'::text[]))));
