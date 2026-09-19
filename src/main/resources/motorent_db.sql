-- 🏍️ MOTORENT ĐÀ NẴNG - CƠ SỞ DỮ LIỆU
-- Tạo database: CREATE DATABASE motorent_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE motorent_db;

SET FOREIGN_KEY_CHECKS = 0;

-- ==========================================
-- NHÓM 1: NGƯỜI DÙNG & PHÂN QUYỀN
-- ==========================================

-- 1. Bảng Users
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY,
    role ENUM('CUSTOMER', 'STAFF', 'ADMIN') DEFAULT 'CUSTOMER',
    username VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    gender BOOLEAN DEFAULT FALSE,
    address VARCHAR(255),
    avatar VARCHAR(255),
    id_card VARCHAR(20),
    driver_license VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. Bảng Token Authentication
DROP TABLE IF EXISTS token_authentication;
CREATE TABLE token_authentication (
    token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_value VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    user_id VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 3. Bảng Chi nhánh (Stations)
DROP TABLE IF EXISTS stations;
CREATE TABLE stations (
    station_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    lat DOUBLE,
    lng DOUBLE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng Giấy tờ xác minh (User Documents)
DROP TABLE IF EXISTS user_documents;
CREATE TABLE user_documents (
    doc_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    doc_type ENUM('ID_CARD_FRONT', 'ID_CARD_BACK', 'DRIVER_LICENSE_FRONT', 'DRIVER_LICENSE_BACK') NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    verified_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users(user_id) ON DELETE SET NULL
);


-- ==========================================
-- NHÓM 2: QUẢN LÍ XE MÁY
-- ==========================================

-- 5. Bảng Danh mục xe
DROP TABLE IF EXISTS motorbike_categories;
CREATE TABLE motorbike_categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 6. Bảng Thương hiệu
DROP TABLE IF EXISTS motorbike_brands;
CREATE TABLE motorbike_brands (
    brand_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(255),
    country VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 7. Bảng Xe máy
DROP TABLE IF EXISTS motorbikes;
CREATE TABLE motorbikes (
    motorbike_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    brand_id BIGINT,
    category_id BIGINT,
    station_id BIGINT,
    year VARCHAR(10),
    cc INT,
    transmission VARCHAR(50),
    fuel_consumption VARCHAR(50),
    price_per_day INT NOT NULL,
    price_per_week INT,
    deposit_amount INT DEFAULT 0,
    highlights TEXT,
    description TEXT,
    license_plate VARCHAR(20) UNIQUE,
    odo_km INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    rating DOUBLE DEFAULT 0.0,
    review_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (brand_id) REFERENCES motorbike_brands(brand_id) ON DELETE SET NULL,
    FOREIGN KEY (category_id) REFERENCES motorbike_categories(category_id) ON DELETE SET NULL,
    FOREIGN KEY (station_id) REFERENCES stations(station_id) ON DELETE SET NULL
);

-- 8. Bảng Hình ảnh xe
DROP TABLE IF EXISTS motorbike_images;
CREATE TABLE motorbike_images (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    motorbike_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (motorbike_id) REFERENCES motorbikes(motorbike_id) ON DELETE CASCADE
);


-- ==========================================
-- NHÓM 3: ĐƠN THUÊ & THANH TOÁN
-- ==========================================

-- 9. Bảng Đơn thuê (Rental Orders)
DROP TABLE IF EXISTS rental_orders;
CREATE TABLE rental_orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_code VARCHAR(20) UNIQUE NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    motorbike_id BIGINT NOT NULL,
    staff_id VARCHAR(36),
    pickup_date TIMESTAMP NOT NULL,
    return_date TIMESTAMP NOT NULL,
    actual_return_date TIMESTAMP,
    days INT NOT NULL,
    pickup_type VARCHAR(50) DEFAULT 'STORE',
    pickup_address VARCHAR(255),
    total_price INT NOT NULL,
    deposit_paid INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(user_id),
    FOREIGN KEY (motorbike_id) REFERENCES motorbikes(motorbike_id),
    FOREIGN KEY (staff_id) REFERENCES users(user_id)
);

-- 10. Bảng Hợp đồng (Rental Contracts)
DROP TABLE IF EXISTS rental_contracts;
CREATE TABLE rental_contracts (
    contract_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNIQUE NOT NULL,
    contract_code VARCHAR(50) UNIQUE,
    customer_signature_url VARCHAR(255),
    staff_signature_url VARCHAR(255),
    vehicle_condition_before TEXT,
    vehicle_condition_after TEXT,
    odo_before INT,
    odo_after INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES rental_orders(order_id) ON DELETE CASCADE
);

-- 11. Bảng Thanh toán (Payments)
DROP TABLE IF EXISTS payments;
CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount INT NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'CASH',
    transaction_code VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING',
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES rental_orders(order_id) ON DELETE CASCADE
);

-- 12. Bảng Gia hạn thuê (Rental Extensions)
DROP TABLE IF EXISTS rental_extensions;
CREATE TABLE rental_extensions (
    extension_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    requested_by VARCHAR(36) NOT NULL,
    original_return_date TIMESTAMP,
    new_return_date TIMESTAMP,
    extra_days INT,
    extra_amount INT,
    status VARCHAR(20) DEFAULT 'PENDING',
    reason TEXT,
    approved_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES rental_orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (requested_by) REFERENCES users(user_id),
    FOREIGN KEY (approved_by) REFERENCES users(user_id)
);


-- ==========================================
-- NHÓM 4: TƯƠNG TÁC & VẬN HÀNH
-- ==========================================

-- 13. Bảng Đánh giá (Reviews)
DROP TABLE IF EXISTS reviews;
CREATE TABLE reviews (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    motorbike_id BIGINT NOT NULL,
    rating DOUBLE NOT NULL,
    comment TEXT,
    staff_reply TEXT,
    status VARCHAR(20) DEFAULT 'PUBLISHED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES rental_orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES users(user_id),
    FOREIGN KEY (motorbike_id) REFERENCES motorbikes(motorbike_id)
);

-- 14. Bảng Bảo trì xe (Maintenance Records)
DROP TABLE IF EXISTS maintenance_records;
CREATE TABLE maintenance_records (
    record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    motorbike_id BIGINT NOT NULL,
    reported_by VARCHAR(36) NOT NULL,
    type VARCHAR(50),
    description TEXT,
    cost INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (motorbike_id) REFERENCES motorbikes(motorbike_id) ON DELETE CASCADE,
    FOREIGN KEY (reported_by) REFERENCES users(user_id)
);

-- 15. Bảng Khuyến mãi (Promotions)
DROP TABLE IF EXISTS promotions;
CREATE TABLE promotions (
    promotion_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(150),
    description TEXT,
    discount_type VARCHAR(20) DEFAULT 'PERCENTAGE',
    discount_value INT NOT NULL,
    min_order_amount INT DEFAULT 0,
    max_discount INT,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    usage_limit INT,
    used_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 16. Bảng Thông báo (Notifications)
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT,
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    link VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ==========================================
-- CHÈN DỮ LIỆU MẪU (MOCK DATA)
-- ==========================================

-- Users
INSERT INTO users (user_id, role, username, password, first_name, last_name, email, phone) VALUES
('u1-admin', 'ADMIN', 'admin', '$2a$10$yO5F7zZkP1h9z3xWlR.2G.2O2oT1dZk4F7zZkP1h9z3xWlR.2G.2O', 'Quản trị', 'Viên', 'admin@motorent.vn', '0999999999'),
('u2-staff', 'STAFF', 'staff01', '$2a$10$yO5F7zZkP1h9z3xWlR.2G.2O2oT1dZk4F7zZkP1h9z3xWlR.2G.2O', 'Nhân', 'Viên', 'staff01@motorent.vn', '0888888888'),
('u3-cust', 'CUSTOMER', 'customer01', '$2a$10$yO5F7zZkP1h9z3xWlR.2G.2O2oT1dZk4F7zZkP1h9z3xWlR.2G.2O', 'Nguyễn', 'Văn An', 'nguyenvanan@email.com', '0901234567');
-- Note: Mật khẩu là chuỗi hash bcrypt tạm, trong code DataInitializer sẽ hash lại "admin123", "staff123", "customer123".

-- Stations
INSERT INTO stations (name, address, phone) VALUES
('Chi nhánh Trung tâm', '15 Lê Lợi, Hải Châu, Đà Nẵng', '0236123456'),
('Chi nhánh Sân bay', 'Nguyễn Văn Linh, Thanh Khê, Đà Nẵng', '0236123457');

-- Brands
INSERT INTO motorbike_brands (name, country) VALUES
('Honda', 'Nhật Bản'),
('Yamaha', 'Nhật Bản'),
('Suzuki', 'Nhật Bản'),
('VinFast', 'Việt Nam');

-- Categories
INSERT INTO motorbike_categories (name) VALUES
('Xe tay ga'),
('Xe côn tay'),
('Xe số'),
('Xe điện');

-- Motorbikes
INSERT INTO motorbikes (name, brand_id, category_id, station_id, year, cc, transmission, fuel_consumption, price_per_day, price_per_week, highlights, license_plate, status, rating, review_count) VALUES
('Honda Air Blade 160 ABS', 1, 1, 1, '2023', 160, 'Tự động', '2.1L/100km', 300000, 1800000, 'ABS 2 kênh | Phanh đĩa trước sau', '43A1-12345', 'AVAILABLE', 4.9, 128),
('Yamaha Exciter 155 VVA RC', 2, 2, 1, '2024', 155, 'Côn tay 6 số', '2.6L/100km', 350000, 2100000, 'VVA Engine | Phanh đĩa ABS', '43A1-23456', 'AVAILABLE', 4.8, 95),
('Honda SH Mode 125 ABS', 1, 1, 2, '2023', 125, 'Tự động', '1.9L/100km', 280000, 1680000, 'Khóa thông minh | Cốp rộng', '43A1-34567', 'AVAILABLE', 4.7, 73),
('Yamaha NVX 155 VVA', 2, 1, 1, '2022', 155, 'Tự động', '2.2L/100km', 270000, 1620000, 'Kết nối Y-Connect', '43A1-45678', 'RENTED', 4.6, 61),
('VinFast Theon S', 4, 4, 2, '2024', 0, 'Tự động', 'Điện - 120km/sạc', 280000, 1680000, 'Pin 4.0kWh | Sạc nhanh 3h', '43A1-56789', 'AVAILABLE', 4.9, 22);

-- Images
INSERT INTO motorbike_images (motorbike_id, image_url, is_primary) VALUES
(1, 'images/mock/airblade160.jpg', TRUE),
(2, 'images/mock/exciter155.jpg', TRUE),
(3, 'images/mock/shmode125.jpg', TRUE),
(4, 'images/mock/nvx155.jpg', TRUE),
(5, 'images/mock/theons.jpg', TRUE);

-- Orders
INSERT INTO rental_orders (order_code, customer_id, motorbike_id, staff_id, pickup_date, return_date, days, total_price, status) VALUES
('MR240001', 'u3-cust', 1, 'u2-staff', '2024-09-14 08:00:00', '2024-09-17 18:00:00', 3, 900000, 'COMPLETED'),
('MR240002', 'u3-cust', 2, 'u2-staff', '2024-09-18 09:00:00', '2024-09-20 18:00:00', 2, 700000, 'HANDED_OVER'),
('MR240003', 'u3-cust', 4, NULL, '2024-09-20 10:00:00', '2024-09-22 18:00:00', 2, 540000, 'PENDING');

SET FOREIGN_KEY_CHECKS = 1;
