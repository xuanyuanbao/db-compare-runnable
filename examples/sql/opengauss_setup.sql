-- openGauss 测试脚本
-- 连接到目标库后执行
-- 例如：
--   gsql -d postgres -U testuser -W Test@123 -f examples/sql/opengauss_setup.sql

CREATE SCHEMA IF NOT EXISTS og_db2_1;
CREATE SCHEMA IF NOT EXISTS og_db2_2;

DROP TABLE IF EXISTS og_db2_1.product CASCADE;
DROP TABLE IF EXISTS og_db2_1.order_header CASCADE;
DROP TABLE IF EXISTS og_db2_1.customer CASCADE;

DROP TABLE IF EXISTS og_db2_2.region CASCADE;
DROP TABLE IF EXISTS og_db2_2.txn_log CASCADE;
DROP TABLE IF EXISTS og_db2_2.account CASCADE;

CREATE TABLE og_db2_1.customer (
    customer_id   INTEGER       NOT NULL,
    customer_name VARCHAR(80)   NOT NULL,
    customer_type CHAR(1)       NOT NULL DEFAULT 'N',
    phone         VARCHAR(20),
    email         VARCHAR(100),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id)
);

CREATE TABLE og_db2_1.order_header (
    order_id      INTEGER        NOT NULL,
    customer_id   INTEGER        NOT NULL,
    order_no      VARCHAR(32)    NOT NULL,
    status        VARCHAR(30)    NOT NULL DEFAULT 'NEW',
    order_amount  DECIMAL(12,2)  NOT NULL DEFAULT 0,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id)
);

CREATE TABLE og_db2_2.account (
    account_id     INTEGER        NOT NULL,
    account_no     VARCHAR(30)    NOT NULL,
    account_name   VARCHAR(80)    NOT NULL,
    account_status CHAR(1)        NOT NULL DEFAULT 'A',
    open_date      DATE           NOT NULL,
    PRIMARY KEY (account_id)
);

CREATE TABLE og_db2_2.txn_log (
    txn_id         INTEGER         NOT NULL,
    account_id     INTEGER         NOT NULL,
    txn_no         VARCHAR(30)     NOT NULL,
    txn_amount     DECIMAL(14,4)   NOT NULL DEFAULT 0,
    txn_status     VARCHAR(20)     NOT NULL DEFAULT 'INIT',
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (txn_id)
);

CREATE TABLE og_db2_2.region (
    region_code    VARCHAR(10)    NOT NULL,
    region_name    VARCHAR(50)    NOT NULL,
    enabled_flag   CHAR(1)        NOT NULL DEFAULT 'Y',
    PRIMARY KEY (region_code)
);

INSERT INTO og_db2_1.customer (customer_id, customer_name, customer_type, phone, email)
VALUES
    (1, 'Alice', 'A', '13800000001', 'alice@example.com'),
    (2, 'Bob',   'B', '13800000002', 'bob@example.com');

INSERT INTO og_db2_1.order_header (order_id, customer_id, order_no, status, order_amount)
VALUES
    (1001, 1, 'A-ORD-1001', 'NEW', 128.50),
    (1002, 2, 'A-ORD-1002', 'DONE', 256.00);

INSERT INTO og_db2_2.account (account_id, account_no, account_name, account_status, open_date)
VALUES
    (2001, 'ACC-B-0001', 'Main Account', 'A', DATE '2024-01-01'),
    (2002, 'ACC-B-0002', 'Reserve Account', 'A', DATE '2024-02-01');

INSERT INTO og_db2_2.txn_log (txn_id, account_id, txn_no, txn_amount, txn_status)
VALUES
    (3001, 2001, 'TXN-B-0001', 1000.2500, 'INIT'),
    (3002, 2002, 'TXN-B-0002', 2500.0000, 'DONE');

INSERT INTO og_db2_2.region (region_code, region_name, enabled_flag)
VALUES
    ('NORTH', 'North Region', 'Y'),
    ('SOUTH', 'South Region', 'Y');
