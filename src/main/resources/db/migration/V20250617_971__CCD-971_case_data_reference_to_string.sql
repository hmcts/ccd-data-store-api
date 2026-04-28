-- Change reference column on case_data
ALTER TABLE public.case_data
    ALTER COLUMN reference TYPE VARCHAR(255);
