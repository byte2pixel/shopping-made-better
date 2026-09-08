-- ============================================================
-- pgTAP tests: household membership (SCRUM-260)
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back. h1 creates
-- a household, h2 joins it, h3 stays outside.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(43);

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
   'babababa-baba-baba-baba-bababababa01',
   'authenticated', 'authenticated', 'household-1@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"House 1"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'babababa-baba-baba-baba-bababababa02',
   'authenticated', 'authenticated', 'household-2@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"House 2"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'babababa-baba-baba-baba-bababababa03',
   'authenticated', 'authenticated', 'household-3@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"House 3"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('babababa-baba-baba-baba-bababababa01', 'House 1'),
  ('babababa-baba-baba-baba-bababababa02', 'House 2'),
  ('babababa-baba-baba-baba-bababababa03', 'House 3')
on conflict (id) do nothing;

-- ------------------------------------------------------------
-- Shape and grants
-- ------------------------------------------------------------
select has_column('public', 'households', 'invite_code',
  'households.invite_code exists');

select col_is_unique('public', 'households', 'invite_code',
  'invite_code is unique');

select results_eq(
  $$ select p.proname::text collate "default", p.prosecdef,
            'search_path=public' = any(p.proconfig),
            has_function_privilege('anon', p.oid, 'execute'),
            has_function_privilege('authenticated', p.oid, 'execute')
       from pg_proc p join pg_namespace n on n.oid = p.pronamespace
      where n.nspname = 'public'
        and p.proname in ('household_member_ids', 'create_household',
                          'join_household', 'leave_household')
      order by p.proname $$,
  $$ values ('create_household', true, true, false, true),
            ('household_member_ids', true, true, false, true),
            ('join_household', true, true, false, true),
            ('leave_household', true, true, false, true) $$,
  'helper and RPCs are SECURITY DEFINER, pin search_path, and are authenticated-only');

select has_view('public', 'household_members', 'household_members exists');

select ok(
  not coalesce(
    (select c.reloptions @> array['security_invoker=true']
     from pg_class c join pg_namespace n on n.oid = c.relnamespace
     where n.nspname = 'public' and c.relname = 'household_members'),
    false),
  'household_members runs as its owner');

select ok(
  has_table_privilege('authenticated', 'public.household_members', 'SELECT')
  and not has_table_privilege('anon', 'public.household_members', 'SELECT'),
  'authenticated may read household_members, anon may not');

select ok(
  has_table_privilege('authenticated', 'public.households', 'SELECT'),
  'authenticated may read households');

select ok(
  not has_table_privilege('authenticated', 'public.households', 'INSERT')
  and not has_table_privilege('authenticated', 'public.households', 'DELETE'),
  'households INSERT and DELETE are RPC-only');

select ok(
  has_column_privilege('authenticated', 'public.households', 'name', 'UPDATE')
  and not has_column_privilege('authenticated', 'public.households', 'invite_code', 'UPDATE'),
  'the client may update households.name but not invite_code');

select ok(
  has_column_privilege('authenticated', 'public.profiles', 'display_name', 'UPDATE')
  and has_column_privilege('authenticated', 'public.profiles', 'primary_goal', 'UPDATE')
  and has_column_privilege('authenticated', 'public.profiles', 'auto_adjust_enabled', 'UPDATE')
  and not has_column_privilege('authenticated', 'public.profiles', 'household_id', 'UPDATE')
  and not has_column_privilege('authenticated', 'public.profiles', 'is_household_head', 'UPDATE'),
  'profiles membership columns are RPC-only');

-- ------------------------------------------------------------
-- No JWT
-- ------------------------------------------------------------
select is(
  (select array_agg(m) from public.household_member_ids() m),
  array[null]::uuid[],
  'with no JWT the helper returns one null row');

select throws_ok(
  $$ select * from public.create_household('Casa') $$,
  'P0001', 'not signed in',
  'create_household needs a signed-in caller');

select throws_ok(
  $$ select * from public.join_household('DEMO2026') $$,
  'P0001', 'not signed in',
  'join_household needs a signed-in caller');

select throws_ok(
  $$ select public.leave_household() $$,
  'P0001', 'not signed in',
  'leave_household needs a signed-in caller');

-- ------------------------------------------------------------
-- create_household
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa01","role":"authenticated"}';

select is(
  (select array_agg(m order by m) from public.household_member_ids() m),
  array['babababa-baba-baba-baba-bababababa01']::uuid[],
  'an un-housed caller is their own only member');

select is(
  (select r.name || '|' || length(r.invite_code)::text
   from public.create_household('  Casa  ') r),
  'Casa|8',
  'create_household trims the name and returns an 8-character code');

select throws_ok(
  $$ select * from public.create_household('Otra') $$,
  'P0001', 'already in a household',
  'a second create is refused');

reset role;

select matches(
  set_config('household_test.code',
             (select h.invite_code from public.households h where h.name = 'Casa'),
             true),
  '^[0-9A-F]{8}$',
  'the invite code is eight upper-case hex characters');

select is(
  (select p.household_id::text || '|' || p.is_household_head::text
   from public.profiles p where p.id = 'babababa-baba-baba-baba-bababababa01'),
  (select h.id::text from public.households h where h.name = 'Casa') || '|true',
  'the creator is head of the new household');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa03","role":"authenticated"}';

select throws_ok(
  $$ select * from public.create_household('   ') $$,
  'P0001', 'name required',
  'a blank name is refused');

-- ------------------------------------------------------------
-- join_household
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa02","role":"authenticated"}';

select is(
  (select r.name
   from public.join_household(' ' || lower(current_setting('household_test.code')) || ' ') r),
  'Casa',
  'join_household accepts the code lower-cased with surrounding whitespace');

select throws_ok(
  $$ select * from public.join_household(current_setting('household_test.code')) $$,
  'P0001', 'already in a household',
  'a member cannot join again');

reset role;

select is(
  (select (p.household_id = h.id)::text || '|' || p.is_household_head::text
   from public.profiles p, public.households h
   where p.id = 'babababa-baba-baba-baba-bababababa02' and h.name = 'Casa'),
  'true|false',
  'the joiner is a member, not head');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa03","role":"authenticated"}';

select throws_ok(
  $$ select * from public.join_household('NOPE1234') $$,
  'P0001', 'invalid invite code',
  'an unknown code is refused');

select throws_ok(
  $$ select * from public.join_household(null) $$,
  'P0001', 'invalid invite code',
  'a null code is refused');

-- ------------------------------------------------------------
-- household_member_ids with a household
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa01","role":"authenticated"}';

select is(
  (select array_agg(m order by m) from public.household_member_ids() m),
  array['babababa-baba-baba-baba-bababababa01',
        'babababa-baba-baba-baba-bababababa02']::uuid[],
  'members see each other');

set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa03","role":"authenticated"}';

select is(
  (select array_agg(m order by m) from public.household_member_ids() m),
  array['babababa-baba-baba-baba-bababababa03']::uuid[],
  'an outsider still sees only themselves');

-- ------------------------------------------------------------
-- households row: members read, the head renames
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa02","role":"authenticated"}';

select is(
  (select count(*)::int from public.households),
  1,
  'a member sees exactly their household');

set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa03","role":"authenticated"}';

select is(
  (select count(*)::int from public.households),
  0,
  'an outsider sees no household');

set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa01","role":"authenticated"}';

update public.households set name = 'Casa 2';

select throws_ok(
  $$ update public.households set invite_code = 'X' $$,
  '42501', null,
  'invite_code cannot be updated from the client');

reset role;

select is(
  (select h.name from public.households h
   where h.invite_code = current_setting('household_test.code')),
  'Casa 2',
  'the head can rename the household');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa02","role":"authenticated"}';

update public.households set name = 'Nope';

reset role;

select is(
  (select h.name from public.households h
   where h.invite_code = current_setting('household_test.code')),
  'Casa 2',
  'a member cannot rename the household');

-- ------------------------------------------------------------
-- household_members view
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa02","role":"authenticated"}';

select is(
  (select string_agg(m.display_name || ':' || m.is_household_head::text || ':' || m.is_self::text,
                     ',' order by m.display_name)
   from public.household_members m),
  'House 1:true:false,House 2:false:true',
  'household_members lists both members with is_self on the caller');

set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa03","role":"authenticated"}';

select is(
  (select string_agg(m.display_name || ':' || m.is_household_head::text || ':' || m.is_self::text,
                     ',' order by m.display_name)
   from public.household_members m),
  'House 3:false:true',
  'an outsider lists only themselves');

-- ------------------------------------------------------------
-- profiles membership columns are RPC-only
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa02","role":"authenticated"}';

select throws_ok(
  $$ update public.profiles set household_id = null $$,
  '42501', null,
  'household_id cannot be updated from the client');

select throws_ok(
  $$ update public.profiles set is_household_head = true $$,
  '42501', null,
  'is_household_head cannot be updated from the client');

select lives_ok(
  $$ update public.profiles set display_name = 'House 2', auto_adjust_enabled = false $$,
  'other profile columns stay writable');

-- ------------------------------------------------------------
-- leave_household
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa01","role":"authenticated"}';

select throws_ok(
  $$ select public.leave_household() $$,
  'P0001', 'head cannot leave while other members remain',
  'the head is blocked while a member remains');

set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa02","role":"authenticated"}';

select lives_ok(
  $$ select public.leave_household() $$,
  'a member can leave');

reset role;

select is(
  (select p.household_id is null and not p.is_household_head
   from public.profiles p where p.id = 'babababa-baba-baba-baba-bababababa02'),
  true,
  'leaving clears the membership columns');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa01","role":"authenticated"}';

select lives_ok(
  $$ select public.leave_household() $$,
  'the head can leave once alone');

reset role;

select ok(
  (select count(*) = 0 from public.households h
   where h.invite_code = current_setting('household_test.code'))
  and (select p.household_id is null and not p.is_household_head
       from public.profiles p where p.id = 'babababa-baba-baba-baba-bababababa01'),
  'the last member out deletes the household and is cleared');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"babababa-baba-baba-baba-bababababa03","role":"authenticated"}';

select throws_ok(
  $$ select public.leave_household() $$,
  'P0001', 'not in a household',
  'an outsider cannot leave');

reset role;

select * from finish();
rollback;
