CREATE OR REPLACE FUNCTION luhn_generate_checkdigit(bigint) RETURNS bigint AS $$
SELECT
  -- Add the digits, doubling even-numbered digits (counting left
  -- with least-significant as zero). Subtract the remainder of
  -- dividing the sum by 10 from 10, and take the remainder
  -- of dividing that by 10 in turn.
  ((BIGINT '10' - SUM(doubled_digit / BIGINT '10' + doubled_digit % BIGINT '10') %
                BIGINT '10') % BIGINT '10')::BIGINT
FROM (SELECT
        -- Extract digit `n' counting left from least significant\
        -- as zero
        MOD( ($1::bigint / (10^n)::bigint), 10::bigint )
        -- double even-numbered digits
        * (2 - MOD(n,2))
          AS doubled_digit
      FROM generate_series(0, CEIL(LOG($1))::INTEGER - 1) AS n
     ) AS doubled_digits;

$$ LANGUAGE SQL
IMMUTABLE
STRICT;

CREATE OR REPLACE FUNCTION luhn_generate(bigint) RETURNS bigint AS $$
SELECT 10 * $1 + luhn_generate_checkdigit($1);
$$ LANGUAGE SQL
IMMUTABLE
STRICT;

UPDATE case_data SET reference = luhn_generate((split_part('' || extract(epoch from now()) * 10, '.', 1) || LPAD('' || id, 4, '0'))::bigint);

DROP FUNCTION luhn_generate(bigint);
DROP FUNCTION luhn_generate_checkdigit(bigint);
