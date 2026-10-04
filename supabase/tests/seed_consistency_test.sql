-- ============================================================
-- pgTAP tests: demo seed consistency (SCRUM-344)
-- ============================================================
-- Run with `npx supabase test db`. Reads the rows dummy_account.sql
-- seeded and changes nothing; the transaction still rolls back.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(3);

select is(
  (select count(*)::int
     from public.purchase_history ph
    where ph.total_amount <> (select coalesce(sum(quantity * price_paid), 0)
                                from public.purchase_history_items
                               where purchase_id = ph.id)),
  0,
  'every seeded trip total equals the sum of its lines');

-- The comparison view is RLS-scoped, so read it as the demo user.
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"11111111-1111-1111-1111-111111111111","role":"authenticated"}';

select is(
  (select ("costHere" = "paidForSameItems")::text || '|' || ("itemsPriced" = "itemsTotal")::text
     from public.purchase_trip_cost_by_store v
     join public.purchase_history ph on ph.id = v."purchaseId"
    where v."storeName" = 'ALDI'
      and ph.purchased_at::date = current_date - 3),
  'true|true',
  'the latest ALDI trip costs today what was paid, with every line priced');

select ok(
  (select "costHere" > "paidForSameItems"
     from public.purchase_trip_cost_by_store v
     join public.purchase_history ph on ph.id = v."purchaseId"
    where v."storeName" = 'ALDI'
      and ph.purchased_at::date = current_date - 96),
  'the 96-day ALDI trip costs more today than was paid');

reset role;

select * from finish();
rollback;
