-- CampusFix MySQL schema (optional: Hibernate also creates/updates tables automatically).
-- Usage:  mysql -u root -p < database/schema.sql
CREATE DATABASE IF NOT EXISTS campusfix CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campusfix;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    student_id  VARCHAR(30)  NOT NULL,
    email       VARCHAR(120) NOT NULL,
    department  VARCHAR(100) NOT NULL,
    password    VARCHAR(100) NOT NULL,          -- BCrypt hash, never plain text
    role        VARCHAR(20)  NOT NULL,          -- STUDENT | ADMIN
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_student_id (student_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS staff (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS complaints (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    title         VARCHAR(150)  NOT NULL,
    description   VARCHAR(2000) NOT NULL,
    category      VARCHAR(30)   NOT NULL,
    priority      VARCHAR(10)   NOT NULL,       -- LOW | MEDIUM | HIGH
    status        VARCHAR(20)   NOT NULL,       -- SUBMITTED ... REJECTED
    location      VARCHAR(150)  NOT NULL,
    image_path    VARCHAR(255),
    admin_remarks VARCHAR(1000),
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6),
    student_id    BIGINT        NOT NULL,
    staff_id      BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_complaint_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_complaint_staff   FOREIGN KEY (staff_id)   REFERENCES staff (id),
    INDEX idx_complaint_status (status),
    INDEX idx_complaint_category (category)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS complaint_history (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    remarks      VARCHAR(1000),
    changed_by   VARCHAR(120) NOT NULL,
    changed_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id) ON DELETE CASCADE
) ENGINE=InnoDB;
