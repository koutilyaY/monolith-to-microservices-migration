-- One-off historical backfill (SQL variant of OrdersBackfillRunner).
--
-- Re-emits an outbox event for every existing order so the SAME Debezium CDC pipeline that ships
-- live changes also ships the historical rows. Run this once against the monolith DB before (or
-- during) cutover:
--
--   psql "postgresql://commerce:commerce@localhost:5432/commerce" -f infra/backfill.sql
--
-- Safe to run alongside live traffic: orders-service upserts idempotently by (orderId, version),
-- so a backfilled (old) version can never overwrite a newer live update. See README
-- "Backfill vs live traffic race".

INSERT INTO orders_outbox (id, aggregatetype, aggregateid, type, payload, created_at)
SELECT
    gen_random_uuid()::text,
    'Order',
    o.id,
    'OrderBackfill',
    json_build_object(
        'orderId',          o.id,
        'customerId',       o.customer_id,
        'status',           o.status,
        'totalAmount',      o.total_amount,
        'aggregateVersion', o.aggregate_version
    )::text,
    now()
FROM orders o;
