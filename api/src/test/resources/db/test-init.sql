-- Same as infra/db/init/01-roles.sh: a non-owner login role for the app, so row-level security applies.
create role sevadesk_app login password 'app_test_password';
