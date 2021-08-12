CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_claimantIndType_claimant_last_name ON public.case_data USING btree (btrim(upper((data #>> '{claimantIndType,claimant_last_name}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_claimantIndType_claimant_first_name ON public.case_data USING btree (btrim(upper((data #>> '{claimantIndType,claimant_first_names}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_receiptDate ON public.case_data USING btree (btrim(upper((data #>> '{receiptDate}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_feeGroupReference ON public.case_data USING btree (btrim(upper((data #>> '{feeGroupReference}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_preferredDQPilotCourt ON public.case_data USING btree (btrim(upper((data #>> '{preferredDQPilotCourt}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_multipleReference ON public.case_data USING btree (btrim(upper((data #>> '{multipleReference}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_respondent ON public.case_data USING btree (btrim(upper((data #>> '{respondent}'::text[]))));
