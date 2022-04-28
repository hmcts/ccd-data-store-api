CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_casename ON public.case_data USING btree (btrim(upper((data #>> '{caseName}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_primaryapplicantforenames ON public.case_data USING btree (btrim(upper((data #>> '{primaryApplicantForenames}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_primaryapplicantsurname ON public.case_data USING btree (btrim(upper((data #>> '{primaryApplicantSurname}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_primaryapplicantforenames_primaryapplicantsurname ON public.case_data USING btree (btrim(upper((data #>> '{primaryApplicantForenames}'::text[]))), btrim(upper((data #>> '{primaryApplicantSurname}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_jur_case_type_state_sec_class ON public.case_data USING btree (jurisdiction, case_type_id, state, security_classification);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_casehandedofftolegacysite ON public.case_data USING btree (btrim(upper((data #>> '{caseHandedOffToLegacySite}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_claimant_company ON public.case_data USING btree (btrim(upper((data #>> '{claimant_Company}'::text[]))));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_case_data_claimanttype_claimant_addressuk_postcode ON public.case_data USING btree (btrim(upper((data #>> '{claimantType}'::text[]))), btrim(upper((data #>> '{claimant_addressUK}'::text[]))),btrim(upper((data #>> '{PostCode}'::text[]))));
