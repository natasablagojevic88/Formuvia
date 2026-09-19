SELECT
        tbl.relname AS table,
        trg.tgname AS name
FROM pg_trigger trg
JOIN pg_class tbl     ON tbl.oid = trg.tgrelid
JOIN pg_namespace nsp ON nsp.oid = tbl.relnamespace
WHERE nsp.nspname = current_schema
  AND NOT trg.tgisinternal
ORDER BY tbl.relname, trg.tgname