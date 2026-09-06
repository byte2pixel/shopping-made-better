-- ============================================================
-- pgTAP tests: undo_inventory_adjustment() and the reverses column
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back, so the
-- fixtures and table clears below never persist. now() is frozen per
-- transaction, so a stamp equal to now() proves the call stamped the lot.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(22);

-- ------------------------------------------------------------
-- Fixtures. Clear the function's output so seeded data cannot shift
-- the counts.
-- ------------------------------------------------------------
delete from public.inventory_adjustments;
delete from public.inventory_items;

-- Two users so RLS can be exercised. GoTrue token columns must be ''
-- rather than NULL.
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
   'abababab-abab-abab-abab-ababababab01',
   'authenticated', 'authenticated', 'undo-adj-1@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Undo Adj 1"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'abababab-abab-abab-abab-ababababab02',
   'authenticated', 'authenticated', 'undo-adj-2@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Undo Adj 2"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('abababab-abab-abab-abab-ababababab01', 'Undo Adj 1'),
  ('abababab-abab-abab-abab-ababababab02', 'Undo Adj 2')
on conflict (id) do nothing;

-- One product; shelf_life_days null keeps the expiry/location triggers inert.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('fafafafa-fafa-fafa-fafa-fafafafafa01', 'TEST-UNDO-01', 9992201,
   'Undo Adj Item', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified');

-- Lots ce01, ce02 and ce04 belong to user 1, ce03 to user 2.
insert into public.inventory_items
  (id, user_id, product_id, quantity, unit, location,
   purchased_at, expires_at, last_auto_adjusted_at, pending_fraction)
values
  -- auto -2 took it from 3 to 1; undo restores 3
  ('cececece-cece-cece-cece-cecececece01', 'abababab-abab-abab-abab-ababababab01',
   'fafafafa-fafa-fafa-fafa-fafafafafa01', 1, 'ea', 'pantry',
   current_date - 10, null, now() - interval '5 days', 0.4000),
  -- confirmed -1: not undoable
  ('cececece-cece-cece-cece-cecececece02', 'abababab-abab-abab-abab-ababababab01',
   'fafafafa-fafa-fafa-fafa-fafafafafa01', 2, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  -- user 2's lot, auto -1 took it from 3 to 2
  ('cececece-cece-cece-cece-cecececece03', 'abababab-abab-abab-abab-ababababab02',
   'fafafafa-fafa-fafa-fafa-fafafafafa01', 2, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  -- apply returns the audit row id
  ('cececece-cece-cece-cece-cecececece04', 'abababab-abab-abab-abab-ababababab01',
   'fafafafa-fafa-fafa-fafa-fafafafafa01', 2, 'ea', 'pantry',
   current_date - 10, null, null, 0);

insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('adadadad-adad-adad-adad-adadadadad01', 'cececece-cece-cece-cece-cecececece01',
   'abababab-abab-abab-abab-ababababab01', -2, 'auto', now() - interval '1 hour'),
  ('adadadad-adad-adad-adad-adadadadad02', 'cececece-cece-cece-cece-cecececece02',
   'abababab-abab-abab-abab-ababababab01', -1, 'confirmed', now() - interval '1 hour'),
  ('adadadad-adad-adad-adad-adadadadad03', 'cececece-cece-cece-cece-cecececece03',
   'abababab-abab-abab-abab-ababababab02', -1, 'auto', now() - interval '1 hour');

-- ------------------------------------------------------------
-- Function and column shape
-- ------------------------------------------------------------
select has_function('public', 'undo_inventory_adjustment',
  array['uuid']::name[],
  'undo_inventory_adjustment(uuid) exists');

select ok(
  (select not p.prosecdef
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'undo_inventory_adjustment'),
  'function is SECURITY INVOKER');

select ok(
  (select 'search_path=public' = any(p.proconfig)
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'undo_inventory_adjustment'),
  'function sets search_path = public');

select ok(
  has_function_privilege('authenticated',
    'public.undo_inventory_adjustment(uuid)', 'EXECUTE'),
  'authenticated can execute');

select has_column('public', 'inventory_adjustments', 'reverses',
  'inventory_adjustments.reverses exists');

select fk_ok('public', 'inventory_adjustments', 'reverses',
  'public', 'inventory_adjustments', 'id',
  'reverses references inventory_adjustments.id');

select has_index('public', 'inventory_adjustments', 'idx_adjustments_reverses',
  'idx_adjustments_reverses exists');

select ok(
  (select i.indexdef like '%UNIQUE%' and i.indexdef like '%WHERE (reverses IS NOT NULL)%'
   from pg_indexes i
   where i.schemaname = 'public' and i.indexname = 'idx_adjustments_reverses'),
  'idx_adjustments_reverses is a partial unique index');

-- ------------------------------------------------------------
-- apply_inventory_adjustment returns the audit row it wrote
-- ------------------------------------------------------------
create temp table applied as
  select * from public.apply_inventory_adjustment(
    'cececece-cece-cece-cece-cecececece04', -1, 'confirmed');

select is(
  (select a.id from public.inventory_adjustments a
   where a.inventory_item_id = 'cececece-cece-cece-cece-cecececece04'),
  (select applied.adjustment_id from applied),
  'apply returns the id of the audit row it inserted');

-- ------------------------------------------------------------
-- undo of an auto -2: restores 3, resets pending, links the rows
-- ------------------------------------------------------------
select ok(
  (select r.delta = 2 and r.new_quantity = 3
      and r.inventory_item_id = 'cececece-cece-cece-cece-cecececece01'
   from public.undo_inventory_adjustment('adadadad-adad-adad-adad-adadadadad01') r),
  'undo of auto -2 on 1 returns delta 2 and new quantity 3');

select is(
  (select ii.quantity::text || '|' || ii.pending_fraction::text || '|'
          || (ii.last_auto_adjusted_at = now())::text
   from public.inventory_items ii
   where ii.id = 'cececece-cece-cece-cece-cecececece01'),
  '3.000|0.0000|true',
  'undo: quantity 3, pending fraction reset, lot stamped');

select is(
  (select a.delta::text || '|' || a.reason || '|' || a.reverses::text
   from public.inventory_adjustments a
   where a.inventory_item_id = 'cececece-cece-cece-cece-cecececece01'
     and a.reason = 'undo'),
  '2.000|undo|adadadad-adad-adad-adad-adadadadad01',
  'undo row records +2 and points at the auto row');

-- ------------------------------------------------------------
-- One undo per row
-- ------------------------------------------------------------
select throws_ok(
  $$ select public.undo_inventory_adjustment('adadadad-adad-adad-adad-adadadadad01') $$,
  'P0001', 'Adjustment adadadad-adad-adad-adad-adadadadad01 already undone',
  'undoing the same row twice is rejected');

select throws_ok(
  $$ insert into public.inventory_adjustments
       (inventory_item_id, user_id, delta, reason, reverses)
     values ('cececece-cece-cece-cece-cecececece01',
             'abababab-abab-abab-abab-ababababab01', 2, 'undo',
             'adadadad-adad-adad-adad-adadadadad01') $$,
  '23505', null,
  'a second row cannot reverse the same adjustment');

-- ------------------------------------------------------------
-- Only auto rows are undoable; unknown ids are not found
-- ------------------------------------------------------------
select throws_ok(
  $$ select public.undo_inventory_adjustment('adadadad-adad-adad-adad-adadadadad02') $$,
  'P0001', 'Adjustment adadadad-adad-adad-adad-adadadadad02 is not an automatic adjustment',
  'a confirmed row cannot be undone');

select throws_ok(
  $$ select public.undo_inventory_adjustment('adadadad-adad-adad-adad-adadadadad99') $$,
  'P0001', 'Adjustment adadadad-adad-adad-adad-adadadadad99 not found or not accessible',
  'an unknown adjustment id is not found');

select ok(
  (select ii.quantity = 2 from public.inventory_items ii
   where ii.id = 'cececece-cece-cece-cece-cecececece02')
  and (select count(*) = 1 from public.inventory_adjustments a
       where a.inventory_item_id = 'cececece-cece-cece-cece-cecececece02'),
  'rejected undos leave the lot and its audit trail untouched');

-- ------------------------------------------------------------
-- RLS: user 1 cannot undo user 2's row; user 2 can
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"abababab-abab-abab-abab-ababababab01","role":"authenticated"}';

select throws_ok(
  $$ select public.undo_inventory_adjustment('adadadad-adad-adad-adad-adadadadad03') $$,
  'P0001', 'Adjustment adadadad-adad-adad-adad-adadadadad03 not found or not accessible',
  'another user''s adjustment is not accessible');

set local request.jwt.claims =
  '{"sub":"abababab-abab-abab-abab-ababababab02","role":"authenticated"}';

select ok(
  (select r.new_quantity = 3
   from public.undo_inventory_adjustment('adadadad-adad-adad-adad-adadadadad03') r),
  'a user can undo their own automatic adjustment under RLS');

reset role;

select is(
  (select a.user_id::text || '|' || a.reverses::text
   from public.inventory_adjustments a
   where a.inventory_item_id = 'cececece-cece-cece-cece-cecececece03'
     and a.reason = 'undo'),
  'abababab-abab-abab-abab-ababababab02|adadadad-adad-adad-adad-adadadadad03',
  'undo row belongs to the lot owner and links the auto row');

-- ------------------------------------------------------------
-- Deleting the lot removes the auto row and the undo row that references it
-- ------------------------------------------------------------
select lives_ok(
  $$ delete from public.inventory_items
     where id = 'cececece-cece-cece-cece-cecececece01' $$,
  'the reverses link does not block the lot cascade');

select is(
  (select count(*)::int from public.inventory_adjustments a
   where a.inventory_item_id = 'cececece-cece-cece-cece-cecececece01'),
  0,
  'both audit rows cascade with the lot');

select * from finish();
rollback;
