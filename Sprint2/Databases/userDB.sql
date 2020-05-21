DROP DATABASE IF EXISTS userdb;
CREATE DATABASE IF NOT EXISTS userdb;

USE userdb;

DROP TABLE IF EXISTS userInfo;
CREATE TABLE userInfo
(
    id INT NOT NULL AUTO_INCREMENT,
    nif VARCHAR(9) UNIQUE NOT NULL,
    email VARCHAR(50) NOT NULL,
    firstName VARCHAR(20) NOT NULL,
    lastName VARCHAR(20) NOT NULL,
    planType VARCHAR(20) NOT NULL,
    address VARCHAR(100) NOT NULL,
    CONSTRAINT pk_userInfo PRIMARY KEY (id)
);

DROP TABLE IF EXISTS userBalance;
CREATE TABLE userBalance
(
    id INT NOT NULL AUTO_INCREMENT,
    balance DECIMAL (4, 2) NOT NULL,
    blackListed BOOLEAN NOT NULL,
    CONSTRAINT pk_userBalance PRIMARY KEY (id),
    CONSTRAINT fk_userInfo_userBalance FOREIGN KEY (id) REFERENCES userInfo(id) on DELETE CASCADE
);

DROP TABLE IF EXISTS history;
CREATE TABLE history
(
    tripID VARCHAR(100) NOT NULL,
    id INT NOT NULL,
    operatorName VARCHAR(30) NOT NULL,
    time_stamp DATETIME NOT NULL,
    CONSTRAINT pk_history PRIMARY KEY (tripID, time_stamp),
    CONSTRAINT fk_userInfo_history FOREIGN KEY (id) REFERENCES userInfo(id) on DELETE CASCADE
);

DROP TABLE IF EXISTS T0_History;
CREATE TABLE T0_History
(
    tripID VARCHAR(100) NOT NULL,
    time_stamp DATETIME NOT NULL,
    station VARCHAR(15) NOT NULL,
    isCheckIn BOOLEAN NOT NULL,
    CONSTRAINT pk_historyt0 PRIMARY KEY (tripID, time_stamp),
    CONSTRAINT fk_historyt0 FOREIGN KEY (tripID, time_stamp) REFERENCES history(tripID, time_stamp) on DELETE CASCADE
);

DROP TABLE IF EXISTS T1_History;
CREATE TABLE T1_History
(
    tripID VARCHAR(100) NOT NULL,
    time_stamp DATETIME NOT NULL,
    price DECIMAL (4, 2) NOT NULL,
    CONSTRAINT pk_historyt1 PRIMARY KEY (tripID, time_stamp),
    CONSTRAINT fk_historyt1 FOREIGN KEY (tripID, time_stamp) REFERENCES history(tripID, time_stamp) on DELETE CASCADE
);

DROP TABLE IF EXISTS T2_History;
CREATE TABLE T2_History
(
    tripID VARCHAR(100) NOT NULL,
    time_stamp DATETIME NOT NULL,
    time BIGINT NOT NULL,
    price DECIMAL(4, 2) NOT NULL,
    CONSTRAINT pk_historyt2 PRIMARY KEY (tripID, time_stamp),
    CONSTRAINT fk_historyt2 FOREIGN KEY (tripID, time_stamp) REFERENCES history(tripID, time_stamp) on DELETE CASCADE
);