> Tài liệu mô tả luồng trước khi bổ sung IPN và trang kết quả. Luồng mới và kiểm thử: xem MINIO_VNPAY_FIX_REPORT.md.

# LUỒNG VNPAY TRONG SOURCE SÂN BÓNG PRO

Căn cứ source hiện tại ngày 13/10/2026. Đây là giải thích code, không xác nhận giao dịch ngân hàng thật và không thay đổi logic thanh toán.

**Luồng thực tế:** tạo đơn giữ chỗ → tạo giao dịch chờ/URL có chữ ký → chuyển trình duyệt sang VNPay → VNPay chuyển trình duyệt về Return URL → Backend kiểm tra → cập nhật thanh toán và đơn nếu hợp lệ.

**Giới hạn quan trọng:** chưa có Controller xử lý IPN. SecurityConfig cho phép đường dẫn `/api/thanh-toan/vnpay/ipn`, nhưng cho phép truy cập không có nghĩa endpoint đã được triển khai. Không trình bày rằng hệ thống đã nhận IPN server-to-server hoàn chỉnh.

## 1. Các file tham gia

| File | Vai trò |
|---|---|
| `fe/src/pages/GuestPages.jsx` | Chọn phương thức, tạo đơn, nhận URL và chuyển trình duyệt; tiếp tục thanh toán từ lịch sử |
| `fe/src/api/authApi.js` | POST tạo URL VNPay |
| `controller/DatSanController.java` | Tạo đơn, lấy giá và giữ chỗ 15 phút với phương thức online |
| `controller/VNPayController.java` | API tạo URL và nhận Return URL |
| `service/VNPayService.java` | Kiểm tra quyền/trạng thái, tạo tham số, xử lý callback và cập nhật entity |
| `util/VNPayUtil.java` | Sắp xếp/encode tham số và HMAC-SHA512 |
| `config/VNPayConfig.java` | Nạp merchant code, hash secret, URL và cấu hình qua Spring properties |
| `service/BookingGuard.java` | Khóa đơn/sân, kiểm tra hiệu lực giữ chỗ |
| `entity/ThanhToan.java`, `entity/DatSan.java` | Dữ liệu giao dịch và đơn đặt |
| `BookingIntegrationTest.java` | Các kiểm thử callback trên dữ liệu cô lập |

Các đường dẫn Backend ở trên nằm dưới `src/main/java/com/example/becicd/`.

## 2. Khách chọn VNPay và tạo đơn

Trong `handleDatSan`, FE kiểm tra phiên đăng nhập và khung giờ đang TRONG rồi gọi API tạo đơn. Khi phương thức là VNPAY:

```js
const resDatSan = await authApi.taoDatSan(
  fieldId, slotChon.khungGioId, ngayDat, phuongThucThanhToan
);
const resPay = await authApi.taoLinkVNPay(resDatSan.id);
window.location.href = resPay.paymentUrl;
```

Đoạn này là phần chính được rút gọn; source đầy đủ có guard gửi lặp, try/catch và trạng thái loading.

Backend tạo `DatSan` theo transaction, khóa sân/khung giờ, kiểm tra trùng và lấy giá theo bảng giá. Đơn online được đặt `GIU_CHO_TAM`, lưu snapshot `gia`, và có hạn giữ chỗ 15 phút. Tiền mặt/chuyển khoản đi nhánh khác nên không nên gom tất cả phương thức vào giữ chỗ online.

Tạo đơn và tạo URL là hai request riêng. Nếu đơn đã tạo nhưng request URL lỗi, FE báo rõ đơn đã có và chuyển về lịch sử để tiếp tục; không tạo thêm đơn chỉ để che lỗi URL.

## 3. Gọi API tạo URL

Endpoint:

```text
POST /api/thanh-toan/vnpay/create/{datSanId}
```

Controller yêu cầu role KHACH_HANG. `VNPayService.createPaymentUrl()` có `@Transactional` và gọi `guard.lockBooking(datSanId)`.

Service kiểm tra:

- Người hiện tại đúng chủ đơn, nếu không trả 403.
- Đơn ở `GIU_CHO_TAM` và chưa hết hạn, nếu không trả 409.
- Giá lưu trên đơn khác null và lớn hơn 0.

Số tiền lấy từ `DatSan.gia` của Backend, không nhận số tiền tùy ý từ FE.

## 4. Tạo hoặc tái dùng ThanhToan

Service tìm giao dịch VNPAY đang CHO_THANH_TOAN của đơn; nếu có thì tái dùng, nếu chưa thì tạo mới. Nó gán:

```text
datSan                 = đơn đang thanh toán
soTien / tongTien      = giá snapshot của đơn
phuongThuc             = VNPAY
trangThai              = CHO_THANH_TOAN
maGiaoDichVnpay        = UUID bỏ dấu '-', nếu chưa có
```

Mã trong `maGiaoDichVnpay` này là **mã tham chiếu do website tạo**, dùng làm `vnp_TxnRef`. Nó khác `vnp_TransactionNo`, là mã giao dịch phía VNPay trong callback. Code hiện kiểm tra TransactionNo có dạng số nhưng không lưu nó vào một field riêng.

Nhấn tạo lại URL khi giao dịch còn chờ không tự tăng số dòng ThanhToan. Hạn URL lấy từ hạn giữ chỗ đã lưu, không tự gia hạn thêm 15 phút mỗi lần nhấn lại. Nếu giao dịch cũ đã THAT_BAI và giữ chỗ vẫn hợp lệ, lần tạo URL tiếp theo có thể tạo giao dịch chờ mới với mã tham chiếu khác.

## 5. Tham số và chữ ký

| Tham số | Nguồn/ý nghĩa trong code |
|---|---|
| `vnp_Version`, `vnp_Command` | Config, hiện mặc định 2.1.0/pay trong application.yml |
| `vnp_TmnCode` | Merchant code từ cấu hình |
| `vnp_Amount` | Giá đơn × 100, đổi thành chuỗi số nguyên |
| `vnp_CurrCode` | VND theo cấu hình |
| `vnp_TxnRef` | Mã tham chiếu giao dịch của website |
| `vnp_OrderInfo` | Thanh toan dat san #ID |
| `vnp_ReturnUrl` | Địa chỉ callback Return đã cấu hình |
| `vnp_IpAddr` | Util lấy X-FORWARDED-FOR hoặc remote address |
| `vnp_CreateDate` | Giờ tạo URL theo BookingGuard.now() |
| `vnp_ExpireDate` | Hạn giữ chỗ của đơn |
| `vnp_SecureHash` | Chữ ký HMAC-SHA512 |

Ví dụ minh họa: giá 300.000 VND gửi thành `vnp_Amount=30000000`. Không phải khách phải trả 30 triệu đồng; đây là cách biểu diễn số tiền cho cổng.

`VNPayUtil.buildQueryAndHash()`:

1. Đưa tham số vào TreeMap để sắp xếp theo key.
2. Bỏ value null/rỗng.
3. Encode value và nối các cặp key=value bằng &.
4. Dùng HashSecret ký chuỗi với HmacSHA512.
5. Thêm vnp_SecureHash vào query, ghép với pay URL.

```java
Mac hmac512 = Mac.getInstance("HmacSHA512");
hmac512.init(new SecretKeySpec(
    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
```

Chữ ký chứng thực/tạo khả năng kiểm tra toàn vẹn; không mã hóa hay giấu số tiền trong URL. HashSecret không gửi cho trình duyệt. JWT là token xác thực người dùng, khác secret ký thông điệp VNPay.

## 6. Khách thực hiện thanh toán trên cổng

Backend trả DTO có `paymentUrl`; FE dùng window.location.href để chuyển sang cổng. Hoạt động chọn ngân hàng/QR và xác nhận trên cổng thuộc phía VNPay, không phải các form React của website đang thu thông tin ngân hàng.

Sau đó trình duyệt được chuyển về `vnp_ReturnUrl`. Source hiện trả kết quả dạng chuỗi từ Backend, chưa có flow redirect kết quả về một page React riêng để trình bày đẹp.

## 7. Backend nhận Return URL

Endpoint:

```text
GET /api/thanh-toan/vnpay/return
```

Callback này được phép truy cập không cần JWT vì trình duyệt quay về từ cổng. Điều đó không có nghĩa mọi query đều được tin: service phải kiểm tra chữ ký và dữ liệu.

Controller từ chối nếu một tham số có nhiều value, rồi chuyển các tham số thành Map. `handleReturn()` kiểm tra theo thứ tự:

1. Có vnp_SecureHash.
2. Bỏ SecureHash và SecureHashType khỏi dữ liệu dùng để tính lại chữ ký.
3. Tính lại HMAC bằng cấu hình merchant; so bằng MessageDigest.isEqual.
4. vnp_TmnCode đúng merchant đã cấu hình.
5. vnp_TxnRef tìm được ThanhToan có phương thức VNPAY.
6. Khóa đơn/sân và refresh ThanhToan để xử lý trên trạng thái hiện tại.
7. Amount hợp lệ và đúng payment.soTien × 100; soTien/tongTien cũng đúng snapshot booking.gia.
8. Thành công yêu cầu cả ResponseCode=00, TransactionStatus=00 và TransactionNo là chuỗi số.
9. Xét trạng thái thanh toán, hạn giữ chỗ và đơn hủy trước khi cập nhật.

```java
boolean success = "00".equals(params.get("vnp_ResponseCode"))
    && "00".equals(params.get("vnp_TransactionStatus"))
    && params.get("vnp_TransactionNo") != null
    && params.get("vnp_TransactionNo").matches("[0-9]+");
```

Chỉ nhìn ResponseCode=00 chưa đủ trong implementation này.

## 8. Cập nhật trạng thái và tình huống lỗi

| Tình huống | Hành vi source |
|---|---|
| Thiếu/sai chữ ký, sai merchant, ref không tồn tại, sai amount | false; không xác nhận đơn/giao dịch |
| Callback hợp lệ nhưng kết quả không thành công | ThanhToan đang chờ thành THAT_BAI; đơn không được xác nhận |
| Thành công, payment đang chờ, đơn giữ chỗ còn hạn | ThanhToan thành THANH_CONG; lưu thời gian; DatSan thành DA_XAC_NHAN; xóa hạn giữ chỗ |
| Callback thành công lặp khi payment đã thành công | Trả true nếu không cần hoàn tiền và đơn không bị hủy; không ghi nhận thanh toán lần hai |
| Thành công nhưng đơn hết hạn/đã hủy/không còn GIU_CHO_TAM | Đánh dấu canXuLyHoanTien=true và thời gian theo nhánh; không khôi phục đơn |
| Thành công đến cho giao dịch không còn CHO_THANH_TOAN | Đánh dấu cần xử lý hoàn tiền; không tự chuyển đơn thành công |

Đoạn thành công:

```java
payment.setTrangThai(TrangThaiThanhToan.THANH_CONG);
payment.setThoiGianThanhToan(BookingGuard.now());
booking.setTrangThai(TrangThaiDatSan.DA_XAC_NHAN);
booking.setThoiGianHetHanGiuCho(null);
```

Các entity được quản lý trong transaction nên Hibernate dirty checking lưu cập nhật khi commit, dù nhánh này không gọi save cho mỗi setter.

Quan trọng: callback đến muộn được đánh dấu cần xử lý hoàn tiền nhưng nhánh đó **không tự gán payment thành THANH_CONG**. Flag không phải lệnh gọi API hoàn tiền; chưa có luồng hoàn tiền ngân hàng tự động hoàn chỉnh được xác minh.

Controller Return trả HTTP 200 kèm “Thanh toán thành công” nếu true, ngược lại HTTP 400 kèm “Thanh toán thất bại hoặc sai chữ ký”. Thông báo lỗi này gom nhiều nguyên nhân, có thể bao gồm đơn hết hạn/đã hủy dù cổng báo đã thu tiền; không nên coi thông báo đó là bằng chứng chắc chắn ngân hàng chưa trừ tiền.

## 9. Return và IPN

Theo tài liệu VNPay, Return là chuyển hướng trình duyệt khách; IPN là thông báo kết quả đến hệ thống merchant để kiểm tra/cập nhật và phản hồi RspCode/Message cho server VNPay.

Trong project hiện tại, chỉ thấy method /return. Không có method /ipn ở các Controller, dù SecurityConfig permitAll đường dẫn đó. Vì cập nhật hiện phụ thuộc Return, nếu khách đóng trình duyệt hoặc không quay về thì dữ liệu website có thể chưa được cập nhật theo kết quả phía cổng. Code cũng chưa có flow truy vấn/đối soát giao dịch tự động được triển khai trong các file vừa kiểm tra.

Đây là hạn chế cần nói trung thực, không phải phần đã tự sửa trong yêu cầu giải thích này.

Nguồn chính thức: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html ; https://sandbox.vnpayment.vn/apis/docs/mo-hinh-ket-noi/

## 10. Các test đã có

BookingIntegrationTest kiểm tra trong môi trường cô lập: tái tạo URL không tạo hai payment chờ; sai chữ ký; sai amount dù chữ ký đã tính lại; callback hợp lệ gọi lặp; khách khác không được tạo URL; giữ chỗ hết hạn rồi callback không khôi phục đơn; hủy đơn chặn callback và đánh dấu xử lý hoàn tiền theo nhánh.

Các test tự tạo callback đã ký bằng key test và H2, không gọi ngân hàng/VNPay thật. Không suy ra rằng luồng production, IPN hoặc hoàn tiền thật đã nghiệm thu từ các test này.

## 11. Lời trình bày khoảng một phút

“Khi khách chọn VNPay, hệ thống tạo đơn giữ chỗ có thời hạn 15 phút. Backend lấy số tiền từ giá đã lưu trong đơn, tạo hoặc tái dùng giao dịch đang chờ rồi sinh URL có mã tham chiếu riêng và chữ ký HMAC-SHA512. Frontend chuyển khách sang VNPay để thanh toán. Khi khách quay về Return URL, Backend kiểm tra chữ ký, merchant, số tiền, mã giao dịch và trạng thái đơn. Nếu hợp lệ và giữ chỗ còn hạn, giao dịch được ghi thành công, đơn được xác nhận. Callback lặp không ghi nhận thanh toán thêm; callback đến muộn hoặc sau hủy được đánh dấu cần xử lý hoàn tiền và không khôi phục đơn. Phiên bản hiện tại cập nhật qua Return, chưa hoàn thiện IPN server-to-server và chưa nghiệm thu thanh toán hay hoàn tiền thật.”
