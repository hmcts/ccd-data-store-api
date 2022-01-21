CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_case_access_category ON public.case_data USING btree (btrim(upper((data #>> '{CaseAccessCategory}'::text[]))));
