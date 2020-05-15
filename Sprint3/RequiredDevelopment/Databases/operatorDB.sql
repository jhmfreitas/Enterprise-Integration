DROP DATABASE IF EXISTS operatordb;
CREATE DATABASE IF NOT EXISTS operatordb;

USE operatordb;

CREATE TABLE operator(
    operatorName VARCHAR(100) NOT NULL,
    operatorType VARCHAR(2) NOT NULL,
    price DECIMAL (4, 2),
    CONSTRAINT pk_operator PRIMARY KEY (operatorName)
);

CREATE TABLE discount(
    discountId VARCHAR(100) NOT NULL UNIQUE,
    discountName VARCHAR(100) NOT NULL UNIQUE,
    value INT NOT NULL,
    beginAt DATETIME NOT NULL,
    endAt DATETIME NOT NULL,
    CONSTRAINT pk_discount PRIMARY KEY (discountId)
);

CREATE TABLE planType(
    plan VARCHAR(20) NOT NULL,
    CONSTRAINT pk_planType PRIMARY KEY (plan)
);

CREATE TABLE discount_planType
(
    discountId VARCHAR(100) NOT NULL,
    plan VARCHAR(20) NOT NULL UNIQUE,
    CONSTRAINT pk_discount_planType PRIMARY KEY (discountId,plan),
    CONSTRAINT fk_planType FOREIGN KEY (plan) REFERENCES planType(plan) on DELETE CASCADE
);

CREATE TABLE operator_discount(
    operatorName VARCHAR(100) NOT NULL,
    discountId VARCHAR(100) NOT NULL,
    CONSTRAINT pk_operator_discount PRIMARY KEY (operatorName,discountId),
    CONSTRAINT fk_operator FOREIGN KEY (operatorName) REFERENCES operator(operatorName) on DELETE CASCADE,
    CONSTRAINT fk_discount FOREIGN KEY (discountId) REFERENCES discount(discountId) on DELETE CASCADE
);
