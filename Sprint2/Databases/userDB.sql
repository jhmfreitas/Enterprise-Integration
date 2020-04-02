DROP DATABASE IF EXISTS UserDB;
CREATE DATABASE IF NOT EXISTS UserDB;

USE UserDB;

CREATE TABLE userInfo(
    token VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    firstName VARCHAR(20) NOT NULL,
    lastName VARCHAR(20) NOT NULL,
    planType VARCHAR(20) NOT NULL,
    time_stamp DATETIME NOT NULL,
    CONSTRAINT pk_userInfo PRIMARY KEY (token)
);

CREATE TABLE userBalance(
    token VARCHAR(100) NOT NULL,
    balance INT NOT NULL,
    CONSTRAINT pk_userBalance PRIMARY KEY (token),
    CONSTRAINT fk_userInfo_userBalance FOREIGN KEY (token) REFERENCES userInfo(token) on DELETE CASCADE
);

CREATE TABLE history(
    tripID VARCHAR(100) NOT NULL,
    token VARCHAR(100) NOT NULL,
    operatorName VARCHAR(30) NOT NULL,
    time_stamp DATETIME NOT NULL,
    CONSTRAINT pk_history PRIMARY KEY (tripID, token)
);

CREATE TABLE T0_History(
    tripID VARCHAR(100) NOT NULL,
    time_stamp DATETIME NOT NULL,
    isCheckIn BOOLEAN NOT NULL,
    CONSTRAINT pk_historyt0 PRIMARY KEY (tripID, token),
    CONSTRAINT fk_historyt0 FOREIGN KEY (tripID, time_stamp) REFERENCES history(tripID, time_stamp) on DELETE CASCADE
);

CREATE TABLE T1_History(
    tripID VARCHAR(100) NOT NULL,
    time_stamp DATETIME NOT NULL,
    price DECIMAL (4, 2) NOT NULL,
    CONSTRAINT pk_historyt1 PRIMARY KEY (tripID, token),
    CONSTRAINT fk_historyt1 FOREIGN KEY (tripID, time_stamp) REFERENCES history(tripID, time_stamp) on DELETE CASCADE
);

CREATE TABLE T2_History(
    tripID VARCHAR(100) NOT NULL,
    time_stamp DATETIME NOT NULL,
    time BIGINT NOT NULL,
    CONSTRAINT pk_historyt2 PRIMARY KEY (tripID, token),
    CONSTRAINT fk_historyt2 FOREIGN KEY (tripID, time_stamp) REFERENCES history(tripID, time_stamp) on DELETE CASCADE
);