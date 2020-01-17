delete from case_users;

insert into case_users (case_data_id, user_id)
  values (16, 1);

insert into case_users (case_data_id, user_id, case_role)
  values (1, 89000, '[CREATOR]');

insert into case_users (case_data_id, user_id, case_role)
values (2, 89001, '[DEFENDANT]');

insert into case_users (case_data_id, user_id, case_role)
values (2, 89001, '[SOLICITOR]');

insert into case_users (case_data_id, user_id, case_role)
values (3, 89001, '[CLAIMANT]');
