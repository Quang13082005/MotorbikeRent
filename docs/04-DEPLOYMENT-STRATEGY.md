# Chiến Lược Triển Khai (DEPLOYMENT STRATEGY)

## Mô Hình Triển Khai
Dự án sẽ trải qua 2 môi trường:
1.  **Local Environment:** Developer chạy trực tiếp bằng Maven và MySQL trên máy cá nhân để phát triển.
2.  **AWS Staging Environment:** Môi trường để khách hàng kiểm thử và trải nghiệm trên điện thoại thật.

## AWS Staging Architecture (Đề xuất)
*   **Compute:** AWS EC2 `t2.micro` (Free Tier) chạy Docker container chứa Spring Boot app.
*   **Database:** Aiven.io (MySQL Free) hoặc cài đặt trực tiếp MySQL lên EC2 (để tiết kiệm chi phí/Free Tier).
*   **Domain & SSL:** Sử dụng IP động hoặc một domain miễn phí kết hợp NGINX + Certbot để hỗ trợ HTTPS (Rất cần thiết cho tính năng PWA).
*   **Static Assets:** Cung cấp trực tiếp bởi Spring Boot.

## Quy Trình Cập Nhật (CI/CD Cơ Bản)
*   Khi kết thúc một Phase và được Approve.
*   Code được đẩy lên nhánh `main`.
*   Kéo code về EC2, chạy `mvn clean package` và restart service.

## Kế Hoạch Rollback
*   Luôn tạo file `.jar` backup trước khi update phiên bản mới.
*   Database sẽ không bị phá hủy cấu trúc unless explicitly approved. Rollback bằng cách trỏ service về file `.jar` phiên bản cũ.
