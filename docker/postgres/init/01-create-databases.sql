-- One database per microservice (database-per-service pattern).
-- Runs only on the very first start of the postgres container (empty volume).
CREATE DATABASE jobdb;
CREATE DATABASE computedb;
CREATE DATABASE billingdb;
