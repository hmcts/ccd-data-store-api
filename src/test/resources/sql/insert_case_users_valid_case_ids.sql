delete from case_users;

insert into case_users (case_data_id, user_id)
  values (16, 1);

insert into case_users (case_data_id, user_id, case_role, role_category)
  values (7578590391163133, 89000, '[CREATOR]', 'PROFESSIONAL');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (6375837333991692, 89001, '[DEFENDANT]', 'PROFESSIONAL');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (6375837333991692, 89001, '[SOLICITOR]', 'PROFESSIONAL');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (6375837333991692, 89002, '[DEFENDANT]', 'PROFESSIONAL');

insert into case_users (case_data_id, user_id, case_role, role_category)
values (123456789023, 89001, '[CLAIMANT]','PROFESSIONAL');

insert into case_users (case_data_id, user_id, case_role, role_category)
  values (1983927457663329, '8842-002', '[TEST-ROLE]', 'PROFESSIONAL');
