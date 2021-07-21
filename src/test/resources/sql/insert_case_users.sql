delete from case_users;

insert into case_users (case_data_id, user_id)
  values (16, 1);

insert into case_users (case_data_id, user_id, case_role, role_category)
  values (1, 89000, '[CREATOR]', 'CITIZEN');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (2, 89001, '[DEFENDANT]', 'CITIZEN');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (2, 89001, '[SOLICITOR]', 'PROFESSIONAL');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (3, 89001, '[CLAIMANT]', 'PROFESSIONAL');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (4, 89002, '[CLAIMANT]', 'JUDICIAL');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (5, 89002, '[CLAIMANT]', 'JUDICIAL');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (4, 123, '[DEFENDANT]', 'JUDICIAL');
