# 🏷️ Hệ Thống Đấu Giá Trực Tuyến — Nhóm 3

> Bài tập lớn môn \*\*Lập Trình Nâng Cao\*\* — Đại học Công Nghệ  
> Phát triển hệ thống đấu giá trực tuyến theo mô hình Client–Server với JavaFX và MySQL.

!\[Java](https://img.shields.io/badge/Java-17-orange?logo=java)
!\[JavaFX](https://img.shields.io/badge/JavaFX-17.0.2-blue)
!\[MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
!\[Maven](https://img.shields.io/badge/Build-Maven-red?logo=apachemaven)
!\[CI](https://github.com/<YOUR\_ORG>/<YOUR\_REPO>/actions/workflows/ci.yml/badge.svg)

\---

## 📑 Mục lục

* [Giới thiệu](#giới-thiệu)
* [Công nghệ \& thư viện](#công-nghệ--thư-viện)
* [Yêu cầu môi trường](#yêu-cầu-môi-trường)
* [Cấu trúc thư mục](#cấu-trúc-thư-mục)
* [Hướng dẫn cài đặt \& chạy](#hướng-dẫn-cài-đặt--chạy)
* [Danh sách chức năng đã hoàn thành](#danh-sách-chức-năng-đã-hoàn-thành)
* [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
* [Thiết kế OOP \& Design Pattern](#thiết-kế-oop--design-pattern)
* [Giao thức mạng](#giao-thức-mạng-protocol)
* [Unit Test \& CI/CD](#unit-test--cicd)
* [Thành viên nhóm](#thành-viên-nhóm)
* [Tài liệu \& Video Demo](#tài-liệu--video-demo)

\---

## Giới thiệu

Hệ thống cho phép nhiều người dùng cùng cạnh tranh giá để mua sản phẩm/dịch vụ trong một khoảng thời gian xác định — tương tự mô hình **eBay Auctions**. Người bán đưa sản phẩm lên; giá cuối cùng được quyết định bởi quá trình đấu giá giữa các người mua.

\---

## Công nghệ \& thư viện

|Thành phần|Công nghệ|
|-|-|
|Ngôn ngữ|Java 17|
|UI|JavaFX 17.0.2 + FXML|
|Mạng|Java Sockets (TCP)|
|Serialisation|Jackson Databind 2.16.1 + Java `ObjectOutputStream`|
|Database|MySQL 8.0|
|JDBC|MySQL Connector/J 8.0.33|
|Bảo mật mật khẩu|jBCrypt 0.4|
|Build tool|Maven|
|Unit test|JUnit Jupiter 5.10.2|
|Code style|Google Java Style (Checkstyle)|
|CI/CD|GitHub Actions|

\---

## Yêu cầu môi trường

* **JDK 17** trở lên
* **MySQL 8.0** đang chạy cục bộ
* **Maven 3.8+**

\---

## Cấu trúc thư mục

```
src/
├── main/java/com/auction/
│   ├── client/
│   │   ├── controller/          # JavaFX Controllers (MVC)
│   │   │   ├── UIController.java
│   │   │   ├── ProductController.java
│   │   │   ├── CreateAuctionController.java
│   │   │   ├── MyAuctionsController.java
│   │   │   ├── ProfileController.java
│   │   │   ├── AdminController.java
│   │   │   ├── LoginController.java
│   │   │   └── RegisterController.java
│   │   ├── view/                # FXML files
│   │   ├── network/
│   │   │   └── ServerConnection.java
│   │   ├── SceneManager.java
│   │   └── Main.java
│   │
│   ├── server/
│   │   ├── network/
│   │   │   └── AuctionServer.java
│   │   ├── service/
│   │   │   ├── AuctionManager.java
│   │   │   └── UserService.java
│   │   ├── db/
│   │   │   ├── DatabaseManager.java
│   │   │   ├── AuctionDAO.java
│   │   │   ├── ItemDAO.java
│   │   │   ├── UserDAO.java
│   │   │   └── BidTransactionDAO.java
│   │   └── ServerMain.java
│   │
│   └── shared/
│       ├── model/
│       │   ├── Entity.java  (abstract)
│       │   ├── User.java    (abstract)
│       │   ├── Bidder.java
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
│       │   └── PasswordUtil.java
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
    │   ├── ConcurrencyTest.java
    │   ├── FactoryTest.java
    │   └── UserLoginTest.java
    ├── server/db/
    │   ├── AuctionSecurityTest.java
    │   ├── AuthSystemTest.java
    │   ├── DatabaseTest.java
    │   ├── TwoClientTest.java
    │   └── TestSaveUser.java
    └── SmokeTest.java
```

\---

## Hướng dẫn cài đặt \& chạy

### 1\. Clone repository

```bash
git clone https://github.com/<YOUR\_ORG>/<YOUR\_REPO>.git
cd <YOUR\_REPO>
```

### 2\. Cấu hình database

Mở `src/main/java/com/auction/server/db/DatabaseManager.java` và chỉnh nếu cần:

```java
private static final String DB\_HOST = "jdbc:mysql://localhost:3306/";
private static final String DB\_NAME = "auction\_db";
private static final String USER    = "root";
private static final String PASS    = "";   // đặt mật khẩu MySQL của bạn
```

> \*\*Không cần tạo bảng thủ công.\*\* `DatabaseManager` tự tạo schema đầy đủ khi server khởi động lần đầu.

### 3\. Build dự án

```bash
mvn clean package -DskipTests
```

### 4\. Chạy Server

> ⚠️ Luôn khởi động \*\*Server trước\*\*, sau đó mới chạy Client.

**Windows:**

```cmd
mvn exec:java -Dexec.mainClass="com.auction.server.ServerMain"
```

**Linux / macOS:**

```bash
mvn exec:java -Dexec.mainClass="com.auction.server.ServerMain"
```

Server lắng nghe tại **cổng 1234**. Khi thấy dòng `\[Server] Listening on port 1234...` là server đã sẵn sàng.

### 5\. Chạy Client (JavaFX)

Mở **terminal mới** (giữ nguyên terminal Server), sau đó:

**Windows:**

```cmd
mvn javafx:run
```

**Linux / macOS:**

```bash
mvn javafx:run
```

Hoặc trong IntelliJ IDEA: chạy trực tiếp class `com.auction.client.Main`.

> \*\*Chạy nhiều Client đồng thời:\*\* Mở nhiều terminal riêng biệt và lặp lại lệnh trên ở mỗi terminal. Đây là cách kiểm tra real-time bidding và concurrent bidding.

### 6\. Tài khoản mặc định

Chạy lớp `CreateAdminAccount` trong `src/test/...` để tạo tài khoản Admin ban đầu, hoặc tự đăng ký tài khoản **Bidder / Seller** qua giao diện.

\---

## Danh sách chức năng đã hoàn thành

### ✅ Chức năng bắt buộc

|Nhóm|Chi tiết|Trạng thái|
|-|-|-|
|**Quản lý người dùng**|Đăng ký / đăng nhập; 3 vai trò: Bidder, Seller, Admin|✅ Hoàn thành|
|**Quản lý sản phẩm**|Thêm / sửa / xóa sản phẩm; phân loại Điện tử / Nghệ thuật / Xe cộ|✅ Hoàn thành|
|**Tham gia đấu giá**|Đặt giá cao hơn giá hiện tại; kiểm tra tính hợp lệ; cập nhật người dẫn đầu real-time|✅ Hoàn thành|
|**Kết thúc phiên**|Tự động đóng theo thời gian; xác định người thắng; vòng đời `OPEN → RUNNING → FINISHED → PAID / CANCELLED`|✅ Hoàn thành|
|**Xử lý lỗi \& ngoại lệ**|Giá thấp hơn hiện tại; phiên đã đóng; số dư không đủ; lỗi kết nối|✅ Hoàn thành|
|**GUI JavaFX**|Danh sách phiên; chi tiết sản phẩm + real-time bidding; quản lý Seller; Admin dashboard|✅ Hoàn thành|
|**Thiết kế OOP**|Cây kế thừa Entity → User/Item; Encapsulation, Inheritance, Polymorphism, Abstraction|✅ Hoàn thành|
|**Kiến trúc Client–Server**|TCP Socket; MVC phía Client (FXML + Controller); MVC phía Server (Handler → Service → DAO)|✅ Hoàn thành|
|**Xử lý đồng thời an toàn**|`synchronized(auction)` tránh lost update, race condition|✅ Hoàn thành|
|**Realtime Update**|Observer Pattern + Socket broadcast; không polling|✅ Hoàn thành|
|**Build \& chất lượng mã**|Maven; Google Java Style; refactoring; đặt tên rõ ràng|✅ Hoàn thành|
|**Unit Test (JUnit 5)**|Logic đấu giá, factory, auth, concurrency, security|✅ Hoàn thành|
|**CI/CD**|GitHub Actions: build + test + checkstyle tự động trên mỗi push|✅ Hoàn thành|

### ⭐ Chức năng nâng cao

|Tính năng|Mô tả|Trạng thái|
|-|-|-|
|**Freeze/Hold Balance**|Tiền "đóng băng" khi đặt giá; hoàn tiền tự động khi thua|✅ Hoàn thành|
|**Anti-sniping**|Bid trong 60 giây cuối → tự động gia hạn thêm 60 giây; broadcast `AUCTION\_EXTENDED`|✅ Hoàn thành|
|**Bid History Visualization**|Biểu đồ đường (line chart) giá đấu theo thời gian thực trong màn hình Product|✅ Hoàn thành|
|**Admin Dashboard**|Duyệt / từ chối phiên; khoá tài khoản; đóng phiên thủ công|✅ Hoàn thành|

\---

## Kiến trúc hệ thống

```
┌─────────────────────────────────────┐     TCP Socket     ┌──────────────────────────────────────┐
│           CLIENT (JavaFX)           │ ◄────────────────► │              SERVER                  │
│                                     │   Java Serializ.   │                                      │
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
                                                            │  MySQL — auction\_db                  │
                                                            └──────────────────────────────────────┘
```

> \*\*Nguyên tắc cốt lõi\*\*: Chỉ Server được truy cập Database. Client giao tiếp hoàn toàn qua Socket.

\---

## Thiết kế OOP \& Design Pattern

### Cây kế thừa

```
Entity (abstract)
├── User (abstract)
│   ├── Bidder      — balance, frozenBalance, freeze/unfreeze, chargeWin
│   ├── Seller      — shopName
│   └── Admin
└── Item (abstract)
    ├── Electronics — brand, warrantyMonths
    ├── Art         — artist, yearCreated
    └── Vehicle     — make, mileage
```

### Design Patterns áp dụng

|Pattern|Áp dụng ở đâu|Mục đích|
|-|-|-|
|**Singleton**|`AuctionManager`|Một instance duy nhất quản lý toàn bộ phiên + scheduler|
|**Factory Method**|`ItemFactory`|Tạo `Electronics` / `Art` / `Vehicle` theo `category` string, tránh if-else lặp lại|
|**Observer**|`AuctionObserver` interface|Server broadcast `BID\_UPDATE`, `AUCTION\_CLOSED`, `AUCTION\_EXTENDED` tới tất cả `ClientHandler`|
|**MVC**|Client + Server|FXML (View) + Controller; Handler → Service → DAO|

### Nguyên tắc OOP

* **Encapsulation**: tất cả fields đều `private/protected`, truy cập qua getter/setter
* **Inheritance**: phân cấp rõ ràng `Entity → User → Bidder/Seller/Admin`, `Entity → Item → Electronics/Art/Vehicle`
* **Polymorphism**: `printInfo()` override ở mỗi loại Item; xử lý `User` polymorphically trong server
* **Abstraction**: `Entity`, `User`, `Item` là abstract class; `AuctionObserver` là interface

\---

## Giao thức mạng (Protocol)

Giao tiếp qua `Message` object (Java Serialization), mỗi message gồm `type` (String) và `payload` (Object).

|Nhóm|Command|Hướng|
|-|-|-|
|Auth|`LOGIN`, `REGISTER`, `LOGOUT`|Client → Server|
|Phiên đấu giá|`GET\_AUCTIONS`, `CREATE\_AUCTION`, `UPDATE\_AUCTION`, `CANCEL\_AUCTION`|Client → Server|
|Đặt giá|`PLACE\_BID`|Client → Server|
|Người dùng|`UPDATE\_PROFILE`, `TOP\_UP`, `GET\_MY\_AUCTIONS`, `GET\_MY\_BIDS`|Client → Server|
|Admin|`GET\_ALL\_USERS`, `GET\_ALL\_AUCTIONS`, `APPROVE\_AUCTION`, `REJECT\_AUCTION`, `BAN\_USER`, `ADMIN\_CLOSE\_AUCTION`|Client → Server|
|Broadcast|`BID\_UPDATE`, `AUCTION\_CLOSED`, `AUCTION\_EXTENDED`, `OUTBID\_NOTIFY`|Server → All Clients|

\---

## Unit Test \& CI/CD

### Chạy test

```bash
# Windows / Linux / macOS
mvn test
```

### Test coverage

|Test class|Kiểm tra|
|-|-|
|`ConcurrencyTest`|Race condition khi nhiều thread đặt giá đồng thời|
|`TwoClientTest`|Mô phỏng 2 client kết nối và đặt giá song song|
|`AuctionManagerTest`|Logic đóng phiên, xác định winner|
|`FactoryTest`|`ItemFactory` tạo đúng loại Item theo category|
|`AuthSystemTest`|Đăng ký / đăng nhập / xác thực BCrypt|
|`AuctionSecurityTest`|Ngăn bid thấp hơn giá hiện tại, bid sau khi phiên đóng|

### CI/CD (GitHub Actions)

File `.github/workflows/ci.yml` tự động chạy trên mỗi push/PR vào `main`:

1. Khởi động MySQL 8.0 service
2. Set up JDK 17 (Temurin)
3. Cache Maven packages
4. Chạy `mvn verify` (build + test + checkstyle)
5. Upload báo cáo test (`surefire-reports`) làm artifact

\---

## Thành viên nhóm

|Tên|MSSV|Vai trò chính|
|-|-|-|
|Đào Duy Mạnh|25021865|backend|
|Trịnh Thị Thu Hiền|25021754|backend|
|Phạm Gia Bảo|25021654|fontend|
|Hà Tiến Đạt|25021707|database|

\---



## Tài liệu \& Video Demo

|Loại|Link|
|-|-|
|📄 Báo cáo PDF|[Xem trên Google Drive](LINK_BAO_CAO_PDF)|
|🎬 Video Demo|[Xem trên Google Drive]((https://drive.google.com/file/d/1iaxYUkl_zlDD85rBpR1anBbav9LT_QC5/view?usp=drive_link))|

> \*\*Nội dung video demo (≤ 4 phút):\*\*
> - Cách khởi động Server và nhiều Client
> - Các chức năng chính: đăng ký, đăng nhập, tạo phiên đấu giá, đặt giá
> - Các tình huống kỹ thuật: xử lý lỗi, concurrent bidding, realtime update, anti-sniping

\---

> \*\*Lưu ý\*\*: Dự án được phát triển cho mục đích học thuật trong khuôn khổ môn Lập trình nâng cao — Đại học Công Nghệ.

