# Production migration runbook

1. Stop writes; record row counts and business totals.
2. Take and verify a full `FastGuyDB` backup. Copy it off-host. Test restore to another database.
3. Run `000_preflight_history.sql`, `010_expand_schema.sql`, `020_backfill_legacy.sql`, `030_constraints_indexes.sql`, `035_backend_hardening.sql`, `039_inventory_waste_state.sql`, `040_production_hardening.sql`, `040_validate.sql`, `041_delivery_failure_recovery.sql`, then `041_validate.sql` in order using an account with DDL rights. Rerun `041_delivery_failure_recovery.sql` once to verify its migration-history idempotency guard. Never run `init.sql` against retained data. Do not run retired `database/20260802_backend_hardening.sql`.
4. `041_local_demo_seed.sql` is optional and local-development only. Never run it in production. Opt in per connection with `EXEC sys.sp_set_session_context @key=N'FASTGUY_ALLOW_DEMO_SEED', @value=1;`, then run seed and rerun `040_validate.sql`.
5. Treat any thrown error as a stop. Correct source data or script assumptions; rerun from `000_preflight_history.sql`.
6. Manually inspect unknown roles, duplicate email/SKU/default variants/idempotency keys, product/variant mismatches, malformed modifier CSV/JSON, coupon ownership/order links, order totals/status history, and application reads/writes.
7. Keep `Role`, `FavoriteProduct`, `CartItemModifier`, `OrderItemModifier`, `ClaimedCoupon`, `CouponUsage`, legacy columns, and `DeliveryZone`. Archive/remove only in a separately approved migration after cutover.
8. Smoke-test login, favorites, cart modifiers, checkout, coupons, order history, inventory, payment, staff and shipper workflows.
9. For enforcement-only rollback run `rollback/030_relax_constraints.sql`. For complete rollback, stop writes and restore the verified backup; the migration intentionally has no destructive reverse backfill.

Assumptions: database name is `FastGuyDB`; SQL Server 2016+; source resembles `eb4d216`; `FavoriteProduct` has `user_id`, `product_id`, `created_at`; `OrderItemModifier` has `order_item_modifier_id`, `modifier_option_id`, `group_name`, `option_name`, `price`; optional `ClaimedCoupon` and `CouponUsage` tables follow the legacy schema when present. Dynamic branches run only when their legacy objects exist.
