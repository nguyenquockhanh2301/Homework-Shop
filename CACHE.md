# CACHE.md

Cache strategy summary

- Use `ConcurrentHashMap<Integer, Product>` to cache products keyed by `id`.
- On `findById(id)` check cache first; on miss load from DB and `cache.put(id, product)`.
- On create/update/delete: perform DB operation in transaction then update/remove cache entry.
- Use `version` column for optimistic locking on updates; DAO throws `OptimisticLockException` on conflict.
- Provide `refreshCache()` admin method to reload all products from DB when needed.
- For multi-instance deployments use external invalidation (not implemented in scaffold).
