# TESTS

Integration test checklist

1. Database initialization
- Run `schema.sql` then `sample-data.sql` and verify `products` rows exist.

2. Product CRUD
- GET /products?action=list shows products.
- POST /products?action=save creates product and DB has new row.
- Edit a product and verify `version` increments in DB.
- Simulate concurrent update to validate optimistic lock.

3. Cart persistence
- POST /cart?action=add&id=1&qty=2 creates cart and cart_items rows; session contains `cartId`.
- Restart Tomcat and ensure cart can be reloaded via `session_id`.

4. Cache
- Verify `findById` uses cache for repeated reads (manual timing).
- Force `refreshCache()` and verify DB changes are picked up.
.