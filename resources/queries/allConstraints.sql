SELECT
        tbl.relname AS table,
        con.conname AS name,
        CASE con.contype
                WHEN 'p' THEN 'PRIMARY KEY'
                WHEN 'u' THEN 'UNIQUE'
                WHEN 'c' THEN 'CHECK'
                WHEN 'f' THEN 'FOREIGN_KEY'
                WHEN 'x' THEN 'EXCLUDE'
        END AS type, 
        pg_get_constraintdef(con.oid) AS definition
  FROM pg_constraint con
  JOIN pg_class tbl     ON tbl.oid = con.conrelid
  JOIN pg_namespace nsp ON nsp.oid = tbl.relnamespace
  WHERE nsp.nspname = current_Schema
    AND con.contype IN ('u', 'c', 'f')
  ORDER BY tbl.relname, con.contype, con.conname;