-- insert admin (username a, password aa)
INSERT INTO "PUBLIC"."USUARIOS" VALUES
(TRUE, NULL, NULL, 1, 'Eloy', 'Velasco', NULL, NULL, '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 'ADMIN', NULL, 'a'),
(TRUE, NULL, NULL, 2, 'User', 'Dos', NULL, NULL, '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 'USER', NULL, 'b');   

-- start id numbering from a value that is larger than any assigned above
ALTER SEQUENCE "PUBLIC"."GEN" RESTART WITH 1024;
