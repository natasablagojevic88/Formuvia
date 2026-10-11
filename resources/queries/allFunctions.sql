SELECT routine_name, data_type
FROM information_schema.routines
WHERE routine_catalog = current_catalog
  AND routine_schema  = current_schema
  AND routine_type = ?
  AND data_type LIKE ?