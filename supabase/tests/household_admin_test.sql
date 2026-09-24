-- ============================================================
-- pgTAP tests: household management RPCs (SCRUM-292)
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back. h1 heads a
-- household with h2 and h3 as members; x is outside it.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(26);

-- ------------------------------------------------------------
-- Fixtures. GoTrue token columns must be '' rather than NULL.
-- ------------------------------------------------------------
insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password,
  email_confirmed_at, created_at, updated_at,
  raw_app_meta_data, raw_user_meta_data,
  confirmation_token, recovery_token,
  email_change, email_change_token_new, email_change_token_current,
  phone_change, phone_change_token, reauthentication_token
)
values
  ('00000000-0000-0000-0000-000000000000',
   'acacacac-acac-acac-acac-acacacacac01',
   'authenticated', 'authenticated', 'admin-head@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Admin Head"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'acacacac-acac-acac-acac-acacacacac02',
   'authenticated', 'authenticated', 'admin-member-2@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Admin Member 2"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'acacacac-acac-acac-acac-acacacacac03',
   'authenticated', 'authenticated', 'admin-member-3@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Admin Member 3"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'acacacac-acac-acac-acac-acacacacac04',
   'authenticated', 'authenticated', 'admin-outsider@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Admin Outsider"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('acacacac-acac-acac-acac-acacacacac01', 'Admin Head'),
  ('acacacac-acac-acac-acac-acacacacac02', 'Admin Member 2'),
  ('acacacac-acac-acac-acac-acacacacac03', 'Admin Member 3'),
  ('acacacac-acac-acac-acac-acacacacac04', 'Admin Outsider')
on conflict (id) do nothing;

insert into public.households (id, name, invite_code)
values ('a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a501', 'Admin House', 'ADMIN001');

update public.profiles
   set household_id = 'a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a501',
       is_household_head = (id = 'acacacac-acac-acac-acac-acacacacac01')
 where id in ('acacacac-acac-acac-acac-acacacacac01',
              'acacacac-acac-acac-acac-acacacacac02',
              'acacacac-acac-acac-acac-acacacacac03');

-- ------------------------------------------------------------
-- Shape and grants
-- ------------------------------------------------------------
select results_eq(
  $$ select p.proname::text collate "default", p.prosecdef,
            'search_path=public' = any(p.proconfig),
            has_function_privilege('anon', p.oid, 'execute'),
            has_function_privilege('authenticated', p.oid, 'execute')
       from pg_proc p join pg_namespace n on n.oid = p.pronamespace
      where n.nspname = 'public'
        and p.proname in ('transfer_household_head', 'remove_household_member',
                          'regenerate_invite_code')
      order by p.proname $$,
  $$ values ('regenerate_invite_code', true, true, false, true),
            ('remove_household_member', true, true, false, true),
            ('transfer_household_head', true, true, false, true) $$,
  'the three RPCs are SECURITY DEFINER, pin search_path, and are authenticated-only');

-- ------------------------------------------------------------
-- No JWT
-- ------------------------------------------------------------
select throws_ok(
  $$ select public.transfer_household_head('acacacac-acac-acac-acac-acacacacac02') $$,
  'P0001', 'not signed in',
  'transfer_household_head needs a signed-in caller');

select throws_ok(
  $$ select public.remove_household_member('acacacac-acac-acac-acac-acacacacac02') $$,
  'P0001', 'not signed in',
  'remove_household_member needs a signed-in caller');

select throws_ok(
  $$ select public.regenerate_invite_code() $$,
  'P0001', 'not signed in',
  'regenerate_invite_code needs a signed-in caller');

-- ------------------------------------------------------------
-- An outsider
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"acacacac-acac-acac-acac-acacacacac04","role":"authenticated"}';

select throws_ok(
  $$ select public.transfer_household_head('acacacac-acac-acac-acac-acacacacac02') $$,
  'P0001', 'not in a household',
  'an outsider cannot transfer the head role');

select throws_ok(
  $$ select public.remove_household_member('acacacac-acac-acac-acac-acacacacac02') $$,
  'P0001', 'not in a household',
  'an outsider cannot remove a member');

select throws_ok(
  $$ select public.regenerate_invite_code() $$,
  'P0001', 'not in a household',
  'an outsider cannot regenerate a code');

-- ------------------------------------------------------------
-- A member who is not the head
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"acacacac-acac-acac-acac-acacacacac02","role":"authenticated"}';

select throws_ok(
  $$ select public.transfer_household_head('acacacac-acac-acac-acac-acacacacac03') $$,
  'P0001', 'not the head',
  'a member cannot transfer the head role');

select throws_ok(
  $$ select public.remove_household_member('acacacac-acac-acac-acac-acacacacac03') $$,
  'P0001', 'not the head',
  'a member cannot remove a member');

select throws_ok(
  $$ select public.regenerate_invite_code() $$,
  'P0001', 'not the head',
  'a member cannot regenerate the code');

-- ------------------------------------------------------------
-- The head with a bad target
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"acacacac-acac-acac-acac-acacacacac01","role":"authenticated"}';

select throws_ok(
  $$ select public.transfer_household_head('acacacac-acac-acac-acac-acacacacac04') $$,
  'P0001', 'member not in household',
  'the head cannot hand the role to an outsider');

select throws_ok(
  $$ select public.transfer_household_head('acacacac-acac-acac-acac-acacacacac01') $$,
  'P0001', 'member not in household',
  'the head cannot hand the role to themselves');

select throws_ok(
  $$ select public.remove_household_member('acacacac-acac-acac-acac-acacacacac01') $$,
  'P0001', 'cannot remove yourself',
  'the head cannot remove themselves');

select throws_ok(
  $$ select public.remove_household_member('acacacac-acac-acac-acac-acacacacac04') $$,
  'P0001', 'member not in household',
  'the head cannot remove an outsider');

-- ------------------------------------------------------------
-- transfer_household_head
-- ------------------------------------------------------------
select lives_ok(
  $$ select public.transfer_household_head('acacacac-acac-acac-acac-acacacacac02') $$,
  'the head can hand the role to a member');

reset role;

select is(
  (select string_agg(p.id::text, ',' order by p.id) from public.profiles p
   where p.household_id = 'a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a501' and p.is_household_head),
  'acacacac-acac-acac-acac-acacacacac02',
  'the household has exactly one head and it is the target');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"acacacac-acac-acac-acac-acacacacac01","role":"authenticated"}';

select throws_ok(
  $$ select public.transfer_household_head('acacacac-acac-acac-acac-acacacacac01') $$,
  'P0001', 'not the head',
  'the former head is now a member');

-- ------------------------------------------------------------
-- remove_household_member
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"acacacac-acac-acac-acac-acacacacac02","role":"authenticated"}';

select lives_ok(
  $$ select public.remove_household_member('acacacac-acac-acac-acac-acacacacac03') $$,
  'the new head can remove a member');

reset role;

select is(
  (select p.household_id is null and not p.is_household_head
   from public.profiles p where p.id = 'acacacac-acac-acac-acac-acacacacac03'),
  true,
  'removal clears the membership columns');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"acacacac-acac-acac-acac-acacacacac03","role":"authenticated"}';

select is(
  (select array_agg(m order by m) from public.household_member_ids() m),
  array['acacacac-acac-acac-acac-acacacacac03']::uuid[],
  'a removed member sees only themselves');

-- ------------------------------------------------------------
-- regenerate_invite_code
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"acacacac-acac-acac-acac-acacacacac02","role":"authenticated"}';

select matches(
  set_config('household_admin_test.code', public.regenerate_invite_code(), true),
  '^[0-9A-F]{8}$',
  'the head gets a new eight-character upper-case hex code');

select isnt(
  current_setting('household_admin_test.code'),
  'ADMIN001',
  'the new code differs from the old one');

reset role;

select is(
  (select h.invite_code from public.households h
   where h.id = 'a5a5a5a5-a5a5-a5a5-a5a5-a5a5a5a5a501'),
  current_setting('household_admin_test.code'),
  'the household row holds the returned code');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"acacacac-acac-acac-acac-acacacacac03","role":"authenticated"}';

select throws_ok(
  $$ select * from public.join_household('ADMIN001') $$,
  'P0001', 'invalid invite code',
  'the old code no longer joins');

select lives_ok(
  $$ select * from public.join_household(current_setting('household_admin_test.code')) $$,
  'the new code joins');

-- ------------------------------------------------------------
-- The former head can now leave
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"acacacac-acac-acac-acac-acacacacac01","role":"authenticated"}';

select lives_ok(
  $$ select public.leave_household() $$,
  'the former head can leave while members remain');

reset role;

select * from finish();
rollback;
