-- insert admin (username a, password aa)
INSERT INTO "PUBLIC"."USUARIOS" VALUES
(TRUE, NULL, NULL, 1, 'Eloy', 'Velasco', NULL, NULL, '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 'ADMIN', NULL, 'a'),
(TRUE, NULL, NULL, 2, 'User', 'Dos', NULL, NULL, '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 'USER', NULL, 'b');
INSERT INTO "PUBLIC"."COLAS" VALUES
(NULL, 15, 10, NULL, TIMESTAMP '2026-03-13 13:24:00', TIMESTAMP '2026-04-14 10:30:00', 975, 'Madrid', 'supermercado', NULL);

-- start id numbering from a value that is larger than any assigned above
ALTER SEQUENCE "PUBLIC"."GEN" RESTART WITH 1024;
