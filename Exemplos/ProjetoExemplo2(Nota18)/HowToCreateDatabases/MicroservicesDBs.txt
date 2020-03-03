//DB: CustomerManangementService

DROP DATABASE IF EXISTS CustomerManagementService;
CREATE DATABASE IF NOT EXISTS CustomerManagementService;

USE CustomerManagementService;

CREATE TABLE AccountManager(
topic VARCHAR(100) NOT NULL,
offset BIGINT NOT NULL,
user_id INT NOT NULL,
checkin_ts DATETIME NOT NULL,
checkout_ts DATETIME,
price FLOAT,
discount FLOAT,
PRIMARY KEY (topic, offset)
);

--------------------------------------------------------------

//DB: ServiceOfRevenueDistribution;

DROP DATABASE IF EXISTS ServiceOfRevenueDistribution;
CREATE DATABASE IF NOT EXISTS ServiceOfRevenueDistribution;

USE ServiceOfRevenueDistribution;

CREATE TABLE Settlement(
topic VARCHAR(100) NOT NULL,
offset BIGINT NOT NULL,
total_price FLOAT NOT NULL,
total_discount FLOAT NOT NULL,
revenue FLOAT NOT NULL,
day VARCHAR(100) NOT NULL,
PRIMARY KEY (topic, day)
);
