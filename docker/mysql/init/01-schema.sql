-- ============================================================
-- 商城数据库初始化脚本（MySQL 8）
-- 每个微服务一个独立库：mall_user / mall_product / mall_order
-- ============================================================

CREATE DATABASE IF NOT EXISTS mall_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ---------------------------- 用户库 ----------------------------
USE mall_user;

DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(64)  NOT NULL COMMENT '密码(MD5加盐)',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 说明：用户密码为 MD5(密码 + 用户名) 加盐存储，无法用 SQL 直接构造，
-- 首次使用请在前端「注册」页面创建账号（用户名 3~20 位，密码 6~20 位）。

DROP TABLE IF EXISTS t_user_address;
CREATE TABLE t_user_address
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    user_id        BIGINT       NOT NULL COMMENT '所属用户ID',
    receiver_name  VARCHAR(50)  NOT NULL COMMENT '收货人',
    receiver_phone VARCHAR(20)  NOT NULL COMMENT '收货人手机号',
    address        VARCHAR(255) NOT NULL COMMENT '详细收货地址',
    is_default     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认：0-否 1-是',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户收货地址表';

-- ---------------------------- 商品库 ----------------------------
USE mall_product;

DROP TABLE IF EXISTS t_product;
CREATE TABLE t_product
(
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    name        VARCHAR(100)   NOT NULL COMMENT '商品名称',
    subtitle    VARCHAR(200)   DEFAULT NULL COMMENT '副标题',
    image       VARCHAR(255)   DEFAULT NULL COMMENT '主图',
    price       DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '售价',
    stock       INT           NOT NULL DEFAULT 0 COMMENT '库存',
    description TEXT          DEFAULT NULL COMMENT '商品详情',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品表';

INSERT INTO t_product (name, subtitle, image, price, stock, description, status)
VALUES ('云原生微服务实战', 'Spring Cloud Alibaba 从入门到项目落地',
        'https://picsum.photos/seed/book1/500/500', 89.00, 100, '本书系统讲解微服务架构设计与治理。', 1),
       ('机械键盘 87 键', '客制化手感，办公游戏两不误',
        'https://picsum.photos/seed/kb1/500/500', 349.00, 50, '热插拔轴体，支持三模连接。', 1),
       ('4K 高清显示器', '27 英寸 IPS 广色域',
        'https://picsum.photos/seed/monitor1/500/500', 1599.00, 30, '支持 HDR400，Type-C 90W 反向供电。', 1),
       ('无线蓝牙耳机', '主动降噪，通勤首选',
        'https://picsum.photos/seed/earphone1/500/500', 499.00, 200, '40 小时续航，支持空间音频。', 1),
       ('人体工学办公椅', '久坐不累，护腰护颈',
        'https://picsum.photos/seed/chair1/500/500', 1299.00, 20, '网布透气，四维扶手调节。', 1),
       ('程序员保温杯', '316 不锈钢，24 小时保温',
        'https://picsum.photos/seed/cup1/500/500', 129.00, 500, '一杯一码，容量 500ml。', 1),
       ('便携充电宝', '20000mAh 大容量，自带线',
        'https://picsum.photos/seed/power1/500/500', 159.00, 300, '支持 22.5W 快充，可上飞机。', 1),
       ('智能手表', '血氧心率监测，14 天续航',
        'https://picsum.photos/seed/watch1/500/500', 899.00, 80, '1.85 英寸 AMOLED 大屏。', 1);

-- ---------------------------- 订单库 ----------------------------
USE mall_order;

DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order
(
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    order_no       VARCHAR(64)    NOT NULL COMMENT '订单号',
    user_id        BIGINT         NOT NULL COMMENT '用户ID',
    total_amount   DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    status         TINYINT        NOT NULL DEFAULT 0 COMMENT '状态：0-待支付 1-已支付 2-已取消',
    address        VARCHAR(255)   DEFAULT NULL COMMENT '收货地址',
    receiver_name  VARCHAR(50)    DEFAULT NULL COMMENT '收货人',
    receiver_phone VARCHAR(20)    DEFAULT NULL COMMENT '收货人手机号',
    remark         VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    pay_time       DATETIME       DEFAULT NULL COMMENT '支付时间',
    create_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

DROP TABLE IF EXISTS t_order_item;
CREATE TABLE t_order_item
(
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    order_id      BIGINT         NOT NULL COMMENT '订单ID',
    product_id    BIGINT         NOT NULL COMMENT '商品ID',
    product_name  VARCHAR(100)   DEFAULT NULL COMMENT '商品名称(冗余)',
    product_image VARCHAR(255)   DEFAULT NULL COMMENT '商品图片(冗余)',
    product_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '下单时单价',
    quantity      INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '小计金额',
    create_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单明细表';
