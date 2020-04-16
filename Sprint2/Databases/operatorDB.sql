DROP DATABASE IF EXISTS operatorManagementDB;
CREATE DATABASE IF NOT EXISTS operatorManagementDB;

USE operatorManagementDB;

CREATE TABLE operator(
    operatorId INT NOT NULL AUTO_INCREMENT,
    operatorName VARCHAR(100) NOT NULL UNIQUE,
    operatorType VARCHAR(2) NOT NULL,
    CONSTRAINT pk_operator PRIMARY KEY (operatorId)
);

CREATE TABLE service(
    operatorName VARCHAR(100) NOT NULL UNIQUE,
    serviceId VARCHAR(20) NOT NULL UNIQUE,
    serviceName VARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(4, 2) NOT NULL,
    CONSTRAINT pk_service PRIMARY KEY (serviceId),
    CONSTRAINT fk_operator_service FOREIGN KEY (operatorName) REFERENCES operator(operatorName) on DELETE CASCADE
);

CREATE TABLE discount(
    operatorName VARCHAR(100) NOT NULL UNIQUE,
    serviceId VARCHAR(100) NOT NULL UNIQUE,
    discountId VARCHAR(100) NOT NULL UNIQUE,
    discountName VARCHAR(100) NOT NULL UNIQUE,
    value INT NOT NULL,
    beginAt DATETIME NOT NULL,
    endAt DATETIME NOT NULL,
    appliesOnlyToPass BOOLEAN NOT NULL,
    CONSTRAINT pk_discount PRIMARY KEY (discountId),
    CONSTRAINT fk_service_discount FOREIGN KEY (operatorName,serviceId) REFERENCES service(operatorName,serviceId) on DELETE CASCADE
);