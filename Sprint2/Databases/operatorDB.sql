DROP DATABASE IF EXISTS OperatorDB;
CREATE DATABASE IF NOT EXISTS OperatorDB;

USE OperatorDB;

CREATE TABLE operator(
    id INT NOT NULL IDENTITY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_operator PRIMARY KEY (id)
);

CREATE TABLE service(
    id INT NOT NULL,
    serviceId INT NOT NULL IDENTITY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(4, 2) NOT NULL,
    CONSTRAINT pk_service PRIMARY KEY (id, serviceId),
    CONSTRAINT fk_operator_service FOREIGN KEY (id) REFERENCES operator(id) on DELETE CASCADE
);

CREATE TABLE discount(
    id INT NOT NULL,
    serviceId INT NOT NULL,
    discountId INT NOT NULL IDENTITY,
    value INT NOT NULL,
    CONSTRAINT pk_discount PRIMARY KEY (id, serviceId, discountId),
    CONSTRAINT fk_operator_service FOREIGN KEY (id,serviceId) REFERENCES service(id,serviceId) on DELETE CASCADE
);