DROP DATABASE IF EXISTS operatorManagementDB;
CREATE DATABASE IF NOT EXISTS operatorManagementDB;

USE operatorManagementDB;

CREATE TABLE operator(
    operatorId INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_operator PRIMARY KEY (operatorId)
);

CREATE TABLE service(
    operatorId INT NOT NULL,
    serviceId VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(4, 2) NOT NULL,
    CONSTRAINT pk_service PRIMARY KEY (serviceId),
    CONSTRAINT fk_operator_service FOREIGN KEY (operatorId) REFERENCES operator(operatorId) on DELETE CASCADE
);

CREATE TABLE discount(
    operatorId INT NOT NULL,
    serviceId VARCHAR(100) NOT NULL,
    discountId VARCHAR(100) NOT NULL,
    value INT NOT NULL,
    CONSTRAINT pk_discount PRIMARY KEY (discountId),
    CONSTRAINT fk_service_discount FOREIGN KEY (operatorId,serviceId) REFERENCES service(operatorId,serviceId) on DELETE CASCADE
);