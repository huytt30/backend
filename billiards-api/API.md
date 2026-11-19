# HT Billiards – Đặc tả REST API

Base URL: `/api/v1`. Dữ liệu trao đổi dạng JSON, thời gian theo ISO-8601 (`2026-11-03T10:15:30`).

Xác thực bằng JWT: gửi header `Authorization: Bearer <token>`.
Endpoint `/admin/**` yêu cầu role `ADMIN`; nhóm giỏ hàng, đơn hàng và `/auth/me` yêu cầu đăng nhập.

## Quy ước chung

Lỗi trả về:

```json
{ "status": 400, "message": "Dữ liệu không hợp lệ", "errors": { "email": "Email không hợp lệ" } }
```

`errors` chỉ có khi lỗi validate. Mã lỗi: 400 dữ liệu sai, 401 chưa đăng nhập / token sai,
403 không đủ quyền, 404 không tìm thấy, 409 xung đột (trùng email, hết hàng, sai trạng thái).

Danh sách phân trang (`page` bắt đầu từ 0):

```json
{ "content": [], "page": 0, "size": 12, "totalElements": 40, "totalPages": 4 }
```

## 1. Tài khoản – `/auth`

| Method | Path | Body | Trả về |
|---|---|---|---|
| POST | `/auth/register` | `{fullName, email, phone, password}` | `AuthResponse` |
| POST | `/auth/login` | `{email, password}` | `AuthResponse` |
| GET | `/auth/me` | – | `User` |
| PUT | `/auth/me` | `{fullName, phone, address}` | `User` |
| PUT | `/auth/me/password` | `{currentPassword, newPassword}` | 204 |

```text
AuthResponse { token, expiresAt, user: User }
User         { id, fullName, email, phone, address, role: "CUSTOMER" | "ADMIN", enabled, createdAt }
```

Mật khẩu tối thiểu 6 ký tự. Tài khoản đăng ký mới luôn có role `CUSTOMER`.

## 2. Danh mục, thương hiệu, sản phẩm (công khai)

| Method | Path | Trả về |
|---|---|---|
| GET | `/categories` | `Category[]` dạng cây (chỉ danh mục gốc, kèm `children`) |
| GET | `/brands` | `Brand[]` |
| GET | `/products` | `Page<ProductSummary>` |
| GET | `/products/{slug}` | `ProductDetail` |
| GET | `/products/{slug}/related` | `ProductSummary[]` (tối đa 8, cùng danh mục) |

Tham số `GET /products`:

- `q`: tìm theo tên.
- `category`: slug danh mục, gồm cả danh mục con.
- `brand`: một hoặc nhiều slug, phân tách bằng dấu phẩy.
- `minPrice`, `maxPrice`: lọc theo giá bán thực tế.
- `featured=true`: chỉ lấy sản phẩm nổi bật.
- `sort`: `newest` (mặc định), `price_asc`, `price_desc`, `name`.
- `page`, `size`: phân trang, `size` mặc định 12, tối đa 48.

```text
Category       { id, name, slug, description, imageUrl, parentId, sortOrder, productCount, children: Category[] }
Brand          { id, name, slug, logoUrl, description }
ProductSummary { id, name, slug, thumbnail, price, salePrice, categoryName, brandName, inStock, featured }
ProductDetail  { id, name, slug, sku, shortDescription, description, price, salePrice, thumbnail,
                 images: string[], specs: [{name, value}],
                 category: {id, name, slug}, brand: {id, name, slug} | null,
                 variants: Variant[], featured, active, createdAt }
Variant        { id, name, sku, price, salePrice, stock }
```

Giá trên sản phẩm (`price`, `salePrice`) lấy từ biến thể có giá bán thấp nhất. `salePrice` là `null` nếu không giảm giá.
Biến thể là các phiên bản của cùng sản phẩm, ví dụ cơ 19oz/20oz, bàn 9 feet/8 feet; mỗi sản phẩm có ít nhất 1 biến thể.
`description` là văn bản thuần, các đoạn cách nhau bằng dòng trống.

## 3. Tin tức (công khai)

| Method | Path | Trả về |
|---|---|---|
| GET | `/articles?page&size` | `Page<ArticleSummary>` (chỉ bài đã đăng) |
| GET | `/articles/{slug}` | `Article` |

```text
ArticleSummary { id, title, slug, summary, thumbnail, publishedAt }
Article        { id, title, slug, summary, content, thumbnail, published, publishedAt, createdAt }
```

## 4. Giỏ hàng – `/cart` (cần đăng nhập)

| Method | Path | Body | Trả về |
|---|---|---|---|
| GET | `/cart` | – | `Cart` |
| POST | `/cart/items` | `{variantId, quantity}` | `Cart` (cộng dồn nếu đã có) |
| PATCH | `/cart/items/{itemId}` | `{quantity}` (≥ 1) | `Cart` |
| DELETE | `/cart/items/{itemId}` | – | `Cart` |

```text
Cart     { items: CartItem[], totalQuantity, subtotal }
CartItem { id, variantId, productId, productName, productSlug, variantName, thumbnail,
           unitPrice, quantity, lineTotal, stock }
```

Số lượng vượt tồn kho → 409.

## 5. Đơn hàng – `/orders` (cần đăng nhập)

| Method | Path | Body | Trả về |
|---|---|---|---|
| POST | `/orders` | `{recipientName, phone, address, note, paymentMethod}` | `Order` |
| GET | `/orders/me?page&size` | – | `Page<OrderSummary>` |
| GET | `/orders/me/{code}` | – | `Order` |
| POST | `/orders/me/{code}/cancel` | – | `Order` |

Khi đặt hàng, server lấy toàn bộ giỏ hàng của người dùng và **tự tính giá** từ dữ liệu sản phẩm, không nhận giá từ client.
Server trừ tồn kho rồi xoá giỏ. Phí vận chuyển: 30.000đ, miễn phí khi tạm tính từ 2.000.000đ.

- `paymentMethod`: `COD` | `BANK_TRANSFER`.
- `status`: `PENDING` → `CONFIRMED` → `SHIPPING` → `COMPLETED`; được hủy (`CANCELLED`) khi đang `PENDING` hoặc `CONFIRMED`.
- Khách chỉ tự hủy được đơn `PENDING`. Hủy đơn sẽ hoàn lại tồn kho.

```text
OrderSummary { id, code, status, paymentMethod, total, itemCount, createdAt }
Order        { id, code, status, paymentMethod, recipientName, phone, address, note,
               subtotal, shippingFee, total, createdAt, updatedAt,
               items: [{productName, productSlug, variantName, thumbnail, unitPrice, quantity, lineTotal}],
               customer: {id, fullName, email} }
```

## 6. Quản trị – `/admin` (role ADMIN)

### Tổng quan

`GET /admin/dashboard` →

```text
{ totalRevenue, ordersToday, pendingOrders, totalCustomers, totalProducts,
  ordersByStatus: { PENDING: 3, CONFIRMED: 1, ... },
  revenueByDay: [{date: "2026-11-03", revenue, orders}], // 7 ngày gần nhất; revenue chỉ tính đơn COMPLETED, orders là số đơn đặt trong ngày
  lowStock: [{variantId, productId, productName, variantName, stock, thumbnail}] }   // tồn ≤ 5
```

### Sản phẩm

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/admin/products?q&category&page&size` | `Page<AdminProductRow>`, gồm cả sản phẩm đã ẩn |
| GET | `/admin/products/{id}` | `ProductDetail` |
| POST | `/admin/products` | body `ProductRequest` → `ProductDetail` |
| PUT | `/admin/products/{id}` | body `ProductRequest` → `ProductDetail` |
| DELETE | `/admin/products/{id}` | ẩn sản phẩm (`active=false`), 204 |

```text
AdminProductRow { id, name, slug, thumbnail, categoryName, brandName, price, salePrice, totalStock, active, featured }
ProductRequest  { name, sku, categoryId, brandId?, shortDescription, description, thumbnail, images: string[],
                  specs: [{name, value}], featured, active,
                  variants: [{id?, name, sku, price, salePrice?, stock}] }   // ít nhất 1 biến thể
```

Khi cập nhật: biến thể có `id` sẽ được sửa, không có `id` sẽ được tạo mới.
Biến thể không còn trong danh sách sẽ bị ẩn, không xoá, để giữ lịch sử đơn hàng.

### Danh mục, thương hiệu, bài viết

| Method | Path | Body |
|---|---|---|
| GET | `/admin/categories` | – (danh sách phẳng) |
| POST / PUT `{id}` | `/admin/categories` | `{name, description, imageUrl, parentId?, sortOrder}` |
| DELETE | `/admin/categories/{id}` | 409 nếu còn sản phẩm hoặc danh mục con |
| GET | `/admin/brands` | – |
| POST / PUT `{id}` | `/admin/brands` | `{name, logoUrl, description}` |
| DELETE | `/admin/brands/{id}` | 409 nếu còn sản phẩm |
| GET | `/admin/articles?page&size` | `Page<Article>` (gồm bản nháp) |
| GET | `/admin/articles/{id}` | `Article` |
| POST / PUT `{id}` | `/admin/articles` | `{title, summary, content, thumbnail, published}` |
| DELETE | `/admin/articles/{id}` | 204 |

Slug được sinh tự động từ tên/tiêu đề (bỏ dấu tiếng Việt).

### Đơn hàng và khách hàng

| Method | Path | Body / Trả về |
|---|---|---|
| GET | `/admin/orders?status&q&page&size` | `Page<OrderSummary>` kèm `recipientName`, `phone`; `q` tìm theo mã đơn, tên, SĐT |
| GET | `/admin/orders/{id}` | `Order` |
| PATCH | `/admin/orders/{id}/status` | `{status}` → `Order`, sai luồng trạng thái → 409 |
| GET | `/admin/users?q&page&size` | `Page<User>` |
| PATCH | `/admin/users/{id}/enabled` | `{enabled}` → `User` (không tự khoá chính mình) |

### Tải ảnh

`POST /admin/uploads` (multipart, field `file`, ảnh jpg/png/webp/gif ≤ 5MB) → `{ "url": "/uploads/<tên-file>" }`.
Ảnh được phục vụ tại `GET /uploads/**`. Frontend ghép đường dẫn này với địa chỉ server API.
