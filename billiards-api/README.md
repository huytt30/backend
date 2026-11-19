# HT Billiards – Backend API

REST API cho website bán cơ, bàn và phụ kiện bida.

Công nghệ: Spring Boot 3.5, Java 17, Spring Security + JWT, Spring Data JPA, MySQL (H2 khi chạy thử), Gradle.

## Chức năng

- **Tài khoản:** đăng ký, đăng nhập bằng JWT, xem và sửa hồ sơ, đổi mật khẩu. Tài khoản mới luôn là khách hàng.
- **Sản phẩm:** danh mục nhiều cấp, thương hiệu, sản phẩm có nhiều phiên bản (trọng lượng, kích thước...).
  Mỗi phiên bản có giá và tồn kho riêng. Có tìm kiếm, lọc theo danh mục, thương hiệu, khoảng giá, sắp xếp và phân trang.
- **Giỏ hàng và đặt hàng:**
  - Server tự tính giá, phí ship, trừ tồn kho bằng câu UPDATE có điều kiện nên không bán quá số lượng còn.
  - Hủy đơn sẽ hoàn lại tồn kho.
  - Trạng thái đơn đi theo luồng cố định.
- **Tin tức:** bài viết hướng dẫn, kinh nghiệm chơi bida.
- **Quản trị (role ADMIN):** thống kê, quản lý sản phẩm, danh mục, thương hiệu, đơn hàng, khách hàng, bài viết; upload ảnh.

Chi tiết từng endpoint: [API.md](API.md). Khi server đang chạy, xem thêm Swagger tại `http://localhost:8080/swagger-ui.html`.

## Chạy trên máy cá nhân

Yêu cầu: JDK 17.

```bash
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

Mặc định dùng profile `dev`:

- Cơ sở dữ liệu H2 lưu tại thư mục `data/`.
- Tự tạo dữ liệu mẫu.
- Tài khoản quản trị: `admin@htbilliards.vn` / `Admin@123`.

Xoá thư mục `data/` để tạo lại dữ liệu từ đầu.

## Chạy test

```bash
./gradlew test
```

Có 61 test (JUnit 5, MockMvc, H2), gọi API qua HTTP với đầy đủ Spring Security. Mỗi test tự rollback dữ liệu.

| Nhóm | File test | Nội dung chính |
|---|---|---|
| Tài khoản, JWT | `user/AuthFlowTest`, `user/ProfileApiTest`, `security/TokenSecurityTest` | Đăng ký luôn là CUSTOMER, 401/403, token hết hạn hoặc giả mạo, đổi mật khẩu |
| Sản phẩm | `catalog/CatalogApiTest`, `catalog/AdminCatalogApiTest` | Lọc, sắp xếp, phân trang, ẩn sản phẩm, phiên bản, chặn xoá danh mục hoặc thương hiệu đang dùng |
| Giỏ hàng, đơn hàng | `cart/CartApiTest`, `order/OrderFlowTest`, `order/OrderRulesTest` | Tồn kho, giá tính ở server, phí ship, luồng trạng thái, khách không xem được giỏ/đơn của người khác |
| Quản trị | `admin/AdminApiTest`, `article/ArticleApiTest`, `upload/UploadApiTest` | Khoá tài khoản, thống kê, bài viết nháp/đã đăng, chặn file giả ảnh |

Bảng test case đầy đủ (dùng cho báo cáo) và bộ kiểm thử E2E nằm ở `billiards-web/TESTING.md`.

## Triển khai với MySQL

```bash
cp .env.example .env     # điền JWT_SECRET, ADMIN_PASSWORD, mật khẩu DB
docker compose up -d --build
```

Hoặc chạy file jar với profile `prod` và các biến môi trường:

| Biến | Ý nghĩa |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Kết nối MySQL |
| `JWT_SECRET` | Khoá ký token, tối thiểu 32 ký tự (bắt buộc) |
| `JWT_EXPIRATION` | Thời hạn token, mặc định `7d` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Tạo tài khoản admin ở lần chạy đầu |
| `CORS_ORIGINS` | Domain frontend, phân tách bằng dấu phẩy |
| `UPLOAD_DIR` | Thư mục lưu ảnh |
| `SEED_SAMPLE_DATA` | `true` để tạo dữ liệu mẫu khi DB trống |

Không ghi mật khẩu hay khoá bí mật vào mã nguồn.

## Cấu trúc thư mục

```text
src/main/java/com/da/billiards
├── config     # Security, CORS, cấu hình, dữ liệu mẫu
├── security   # JWT: tạo/đọc token, filter xác thực
├── common     # Xử lý lỗi chung, phân trang, tạo slug
├── user       # Tài khoản, đăng ký/đăng nhập
├── catalog    # Danh mục, thương hiệu, sản phẩm, phiên bản
├── cart       # Giỏ hàng
├── order      # Đơn hàng, luồng trạng thái
├── article    # Tin tức
├── admin      # Thống kê, quản lý khách hàng
└── upload     # Upload ảnh
```

Mỗi package chứa entity, repository, DTO, service và controller của chức năng đó.
