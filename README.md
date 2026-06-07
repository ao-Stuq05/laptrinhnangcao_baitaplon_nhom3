# 🏷️ Hệ Thống Đấu Giá Trực Tuyến — Nhóm 3

> Bài tập lớn môn **Lập Trình Nâng Cao** — Đại học Công Nghệ  
> Phát triển hệ thống đấu giá trực tuyến theo mô hình Client–Server với JavaFX và MySQL.

![Java](https://img.shields.io/badge/Java-17-orange?logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.2-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![Maven](https://img.shields.io/badge/Build-Maven-red?logo=apachemaven)
![CI](https://github.com/<YOUR_ORG>/<YOUR_REPO>/actions/workflows/ci.yml/badge.svg)

---

## 📑 Mục lục

- [Giới thiệu](#giới-thiệu)
- [Tính năng](#tính-năng)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Công nghệ & thư viện](#công-nghệ--thư-viện)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Hướng dẫn cài đặt & chạy](#hướng-dẫn-cài-đặt--chạy)
- [Thiết kế OOP & Design Pattern](#thiết-kế-oop--design-pattern)
- [Giao thức mạng (Protocol)](#giao-thức-mạng-protocol)
- [Unit Test & CI/CD](#unit-test--cicd)
- [Thành viên nhóm](#thành-viên-nhóm)

---

## Giới thiệu

Hệ thống cho phép nhiều người dùng cùng cạnh tranh giá để mua sản phẩm/dịch vụ trong một khoảng thời gian xác định — tương tự mô hình **eBay Auctions**. Người bán đưa sản phẩm lên; giá cuối cùng được quyết định bởi quá trình đấu giá giữa các người mua.

---

## Tính năng

### ✅ Chức năng bắt buộc

| Nhóm | Chi tiết |
|------|----------|
| **Quản lý người dùng** | Đăng ký / đăng nhập; 3 vai trò: Bidder, Seller, Admin |
| **Quản lý sản phẩm** | Thêm / sửa / xóa sản phẩm; hỗ trợ upload ảnh; phân loại Điện tử / Nghệ thuật / Xe cộ |
| **Đấu giá** | Đặt giá cao hơn giá hiện tại; kiểm tra tính hợp lệ; cập nhật người dẫn đầu real-time |
| **Kết thúc phiên** | Tự động đóng theo thời gian; xác định người thắng; vòng đời: `OPEN → RUNNING → FINISHED → PAID / CANCELLED` |
| **Xử lý lỗi** | Giá thấp hơn hiện tại; phiên đã đóng; số dư không đủ; lỗi kết nối |
| **GUI JavaFX** | Danh sách phiên đấu giá; chi tiết sản phẩm + real-time bidding; quản lý của Seller; Admin dashboard |

### ⭐ Chức năng nâng cao

| Tính năng | Mô tả |
|-----------|-------|
| **Freeze/Hold Balance** | Khi đặt giá, tiền được "đóng băng" để đảm bảo thanh toán; người thua được hoàn tiền tự động khi phiên kết thúc |
| **Anti-sniping** | Nếu có bid trong **60 giây cuối**, phiên tự động gia hạn thêm 60 giây; broadcast `AUCTION_EXTENDED` tới tất cả client |
| **Realtime Update (Observer)** | Mọi bid hợp lệ được broadcast ngay lập tức tới tất cả client đang xem — không polling; dùng Java Socket + `CopyOnWriteArrayList` |
| **Concurrent Bidding** | Khóa cấp object (`synchronized` trên `Auction`) để tránh lost update và race condition khi nhiều bidder đặt giá đồng thời |
| **Bid History** | Lịch sử đặt giá theo thời gian thực; biểu đồ đường (line chart) giá đấu trong màn hình Product |
| **Admin Dashboard** | Duyệt / từ chối phiên; khoá tài khoản; đóng phiên thủ công; xem toàn bộ người dùng & phiên đấu giá |

---

## Kiến trúc hệ thống

```
┌─────────────────────────────────────┐     TCP Socket     ┌──────────────────────────────────────┐
│           CLIENT (JavaFX)           │ ◄────────────────► │              SERVER                  │
│                                     │   JSON / Object     │                                      │
│  View (.fxml)                       │                     │  AuctionServer (port 1234)            │
│    └─ Controller (JavaFX MVC)       │                     │    └─ ClientHandler (Thread/client)  │
│         └─ ServerConnection         │                     │         └─ AuctionManager (Singleton)│
│              (Socket + Thread)      │                     │              ├─ ScheduledExecutor    │
│                                     │                     │              └─ Anti-sniping logic   │
│  Shared Model (dùng chung)          │                     │                                      │
│    User / Bidder / Seller / Admin   │                     │  Service Layer                       │
│    Item / Electronics / Art /       │                     │    └─ UserService                    │
│    Vehicle                          │                     │                                      │
│    Auction / BidTransaction         │                     │  DAO Layer (chỉ server truy cập DB)  │
│    AuctionStatus / Message          │                     │    AuctionDAO / ItemDAO /            │
│    Custom Exceptions                │                     │    UserDAO / BidTransactionDAO       │
└─────────────────────────────────────┘                     │                                      │
                                                            │  MySQL — auction_db                  │
                                                            └──────────────────────────────────────┘
```

> **Nguyên tắc cốt lõi**: Chỉ Server được truy cập Database. Client giao tiếp hoàn toàn qua Socket.

---

## Cấu trúc thư mục

```
src/
├── main/java/com/auction/
│   ├── client/
│   │   ├── controller/          # JavaFX Controllers (MVC)
│   │   │   ├── UIController.java          # Danh sách phiên, tìm kiếm, lọc
│   │   │   ├── ProductController.java     # Chi tiết + real-time bidding + chart
│   │   │   ├── CreateAuctionController.java
│   │   │   ├── MyAuctionsController.java  # Lịch sử của Seller
│   │   │   ├── ProfileController.java
│   │   │   ├── AdminController.java
│   │   │   ├── LoginController.java
│   │   │   └── RegisterController.java
│   │   ├── view/                # FXML files
│   │   ├── network/
│   │   │   └── ServerConnection.java      # Socket client + listener thread
│   │   ├── SceneManager.java
│   │   └── Main.java
│   │
│   ├── server/
│   │   ├── network/
│   │   │   └── AuctionServer.java         # Socket server + toàn bộ protocol handler
│   │   ├── service/
│   │   │   ├── AuctionManager.java        # Singleton, quản lý vòng đời phiên + anti-snipe
│   │   │   └── UserService.java
│   │   ├── db/                            # DAO layer
│   │   │   ├── DatabaseManager.java       # Tự tạo schema nếu chưa tồn tại
│   │   │   ├── AuctionDAO.java
│   │   │   ├── ItemDAO.java
│   │   │   ├── UserDAO.java
│   │   │   └── BidTransactionDAO.java
│   │   └── ServerMain.java
│   │
│   └── shared/                            # Dùng chung client + server
│       ├── model/                         # Toàn bộ domain model
│       │   ├── Entity.java  (abstract)
│       │   ├── User.java    (abstract)
│       │   ├── Bidder.java  — freeze/unfreeze balance
│       │   ├── Seller.java
│       │   ├── Admin.java
│       │   ├── Item.java    (abstract)
│       │   ├── Electronics.java / Art.java / Vehicle.java
│       │   ├── ItemFactory.java
│       │   ├── Auction.java
│       │   ├── BidTransaction.java
│       │   ├── AuctionStatus.java
│       │   ├── AuctionObserver.java (interface)
│       │   ├── Message.java
│       │   └── PasswordUtil.java   (BCrypt)
│       ├── exception/
│       │   ├── AuctionClosedException.java
│       │   ├── InvalidBidException.java
│       │   └── AuthenticationException.java
│       └── network/
│           └── Message.java
│
└── test/java/com/auction/
    ├── shared/model/
    │   ├── AuctionManagerTest.java
    │   ├── ConcurrencyTest.java        # Kiểm tra race condition
    │   ├── FactoryTest.java
    │   └── UserLoginTest.java
    ├── server/db/
    │   ├── AuctionSecurityTest.java
    │   ├── AuthSystemTest.java
    │   ├── DatabaseTest.java
    │   ├── TwoClientTest.java          # Mô phỏng 2 client đồng thời
    │   └── TestSaveUser.java
    └── SmokeTest.java
```

---

## Công nghệ & thư viện

| Thành phần | Công nghệ |
|------------|-----------|
| Ngôn ngữ | Java 17 |
| UI | JavaFX 17.0.2 + FXML |
| Mạng | Java Sockets (TCP) |
| Serialisation | Jackson Databind 2.16.1 + Java `ObjectOutputStream` |
| Database | MySQL 8.0 |
| JDBC | MySQL Connector/J 8.0.33 |
| Bảo mật mật khẩu | jBCrypt 0.4 |
| Build tool | Maven |
| Unit test | JUnit Jupiter 5.10.2 |
| Code style | Google Java Style (Checkstyle) |
| CI/CD | GitHub Actions |

---

## Yêu cầu môi trường

- **JDK 17** trở lên
- **MySQL 8.0** đang chạy cục bộ
- **Maven 3.8+**

---

## Hướng dẫn cài đặt & chạy

### 1. Clone repository

```bash
git clone https://github.com/<YOUR_ORG>/<YOUR_REPO>.git
cd <YOUR_REPO>
```

### 2. Cấu hình database

Mở `src/main/java/com/auction/server/db/DatabaseManager.java` và chỉnh nếu cần:

```java
private static final String DB_HOST = "jdbc:mysql://localhost:3306/";
private static final String DB_NAME = "auction_db";
private static final String USER    = "root";
private static final String PASS    = "";          // đặt mật khẩu MySQL của bạn
```

> **Không cần tạo bảng thủ công.** `DatabaseManager` tự tạo schema đầy đủ khi server khởi động lần đầu.

### 3. Build dự án

```bash
mvn clean package -DskipTests
```

### 4. Chạy Server

```bash
mvn exec:java -Dexec.mainClass="com.auction.server.ServerMain"
```

Server lắng nghe tại **cổng 1234**.

### 5. Chạy Client (JavaFX)

Mở terminal mới:

```bash
mvn javafx:run
```

Hoặc trong IntelliJ IDEA: chạy trực tiếp `com.auction.client.Main`.

> Có thể mở nhiều cửa sổ Client để kiểm tra multi-user / real-time bidding.

### 6. Tài khoản mặc định

Chạy lớp `CreateAdminAccount` trong `src/test/...` để tạo tài khoản Admin ban đầu, hoặc tự đăng ký tài khoản Bidder / Seller qua giao diện.

---

## Thiết kế OOP & Design Pattern

### Cây kế thừa

```
Entity (abstract)
├── User (abstract)
│   ├── Bidder      — balance, frozenBalance, freeze/unfreeze, chargeWin
│   ├── Seller      — shopName
│   └── Admin
└── Item (abstract)
    ├── Electronics — brand, warranty
    ├── Art         — artist, year
    └── Vehicle     — brand, mileage
```

### Design Patterns áp dụng

| Pattern | Áp dụng ở đâu |
|---------|---------------|
| **Singleton** | `AuctionManager` — một instance duy nhất quản lý toàn bộ phiên + scheduler |
| **Factory Method** | `ItemFactory` — tạo `Electronics` / `Art` / `Vehicle` theo `category` string |
| **Observer** | `AuctionObserver` interface; Server broadcast `BID_UPDATE`, `AUCTION_CLOSED`, `AUCTION_EXTENDED` tới tất cả `ClientHandler` |
| **MVC** | Client: FXML (View) + Controller; Server: Controller handler → Service → DAO |

### Nguyên tắc OOP

- **Encapsulation**: tất cả fields đều `private/protected`, truy cập qua getter/setter
- **Inheritance**: phân cấp rõ ràng `Entity → User → Bidder/Seller/Admin`, `Entity → Item → Electronics/Art/Vehicle`
- **Polymorphism**: `printInfo()` override ở mỗi loại Item; xử lý `User` polymorphically trong server
- **Abstraction**: `Entity`, `User`, `Item` là abstract class; `AuctionObserver` là interface

---

## Giao thức mạng (Protocol)

Giao tiếp qua `Message` object (Java serialization), mỗi message gồm `type` (String) và `payload` (Object).

| Loại | Command | Hướng |
|------|---------|-------|
| Auth | `LOGIN`, `REGISTER`, `LOGOUT` | Client → Server |
| Phiên đấu giá | `GET_AUCTIONS`, `CREATE_AUCTION`, `UPDATE_AUCTION`, `CANCEL_AUCTION` | Client → Server |
| Đặt giá | `PLACE_BID` | Client → Server |
| Người dùng | `UPDATE_PROFILE`, `TOP_UP`, `GET_MY_AUCTIONS`, `GET_MY_BIDS` | Client → Server |
| Admin | `GET_ALL_USERS`, `GET_ALL_AUCTIONS`, `APPROVE_AUCTION`, `REJECT_AUCTION`, `BAN_USER`, `ADMIN_CLOSE_AUCTION` | Client → Server |
| Broadcast | `BID_UPDATE`, `AUCTION_CLOSED`, `AUCTION_EXTENDED`, `OUTBID_NOTIFY` | Server → All Clients |

---

## Unit Test & CI/CD

### Chạy test

```bash
mvn test
```

### Test coverage

| Test class | Kiểm tra |
|------------|----------|
| `ConcurrencyTest` | Race condition khi nhiều thread đặt giá đồng thời |
| `TwoClientTest` | Mô phỏng 2 client kết nối và đặt giá song song |
| `AuctionManagerTest` | Logic đóng phiên, xác định winner |
| `FactoryTest` | `ItemFactory` tạo đúng loại Item |
| `AuthSystemTest` | Đăng ký / đăng nhập / xác thực BCrypt |
| `AuctionSecurityTest` | Ngăn bid thấp hơn giá hiện tại, bid sau khi phiên đóng |

### CI/CD (GitHub Actions)

File `.github/workflows/ci.yml` tự động chạy trên mỗi push/PR vào `main`:

1. Khởi động MySQL 8.0 service
2. Set up JDK 17 (Temurin)
3. Cache Maven packages
4. Chạy `mvn verify` (build + test + checkstyle)
5. Upload báo cáo test (`surefire-reports`) làm artifact

---

## Thành viên nhóm

| Tên | MSSV | Vai trò chính |
|-----|------|---------------|
| ... | ... | ... |
| ... | ... | ... |
| ... | ... | ... |

---

> **Lưu ý**: Dự án được phát triển cho mục đích học thuật trong khuôn khổ môn Lập trình nâng cao — Đại học Công Nghệ.
