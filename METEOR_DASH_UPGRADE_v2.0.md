# Meteor Dash 2.0 — Báo cáo triển khai

Ngày kiểm tra: 17/10/2026. Phiên bản Android: **2.0**, versionCode **5**.
Thiết kế tham chiếu: [100 màn chơi](METEOR_DASH_V2_100_MAN_THIET_KE.md).

## Nội dung đã tích hợp

- Chiến dịch offline 100 màn, 10 chương; mỗi màn có dữ liệu mục tiêu, thời hạn, mẫu nguy hiểm và điều kiện sao kỹ năng riêng trong mã nguồn.
- Bản đồ mở từng màn theo kết quả thắng, 300 sao; sao phụ cộng dồn độc lập qua các lần chơi. Không yêu cầu đủ ba sao để mở màn tiếp theo.
- Màn chuẩn bị hiển thị tên, điểm đặc biệt, mục tiêu, sao, thưởng lần đầu, tàu đang chọn và chế độ luyện tập. Có lựa chọn tuyến ở 57, 91, 96.
- Mục tiêu hỗn hợp: sống sót, qua cổng, nhận hàng, cứu hộ giữ một giây, né sát, tắt nút bằng xung, cấp năng lượng, hộ tống, bảo vệ trạm và phá lõi.
- Mười cửa ải trùm ở 10, 20…100: lõi đóng/mở, chu kỳ lặp nếu lỡ, pin tiếp tế, nhiều pha. Trùm cuối có nhịp nghỉ, hồi một giáp và nạp giữa các cặp lõi.
- Môi trường gió, quán tính băng, vùng cấm nạp, thời gian khác nhau theo làn, mồi giả, ghi nhớ thứ tự, đá tách mảnh và đá quay ngược.
- 12 tàu với hình dáng, giáp, kích thước va chạm, tốc độ, hồi năng lượng và đặc tính riêng; mua bằng tín dụng kiếm trong game, trang bị chỉ tàu đã sở hữu. Astra có hai cấu hình trước lượt.
- Xưởng hiển thị số dư, thông số, đặc tính, giá và trạng thái sở hữu; không đủ tiền thì khóa mua.
- HUD chiến dịch theo dõi các mục tiêu, giáp đồng minh/trạm, thời hạn và trạng thái lõi. Kết quả có thắng/thua, lý do thất bại, sao và từng khoản thưởng.
- Chế độ sinh tồn vô tận tiếp tục có kỷ lục riêng; luyện tập không trả thưởng hoặc mở khóa.
- Giữ 20 ngôn ngữ của bản 1.3, thêm tên/mô tả 100 màn, chương, tàu và giao diện mới. Không cần mạng khi chơi.
- Thêm ba mức độ nhạy điều khiển. Giới hạn tốc độ di chuyển thực tế dựa trên tàu.

## Tiền, sao và dữ liệu

Thưởng cố định khi hoàn tất lần đầu toàn bộ chiến dịch và đủ sao là **23.900 tín dụng**, chưa tính tín dụng theo điểm mỗi lượt. Công thức đúng theo thiết kế: lần đầu `40 + 12 × chương + 4 × vị trí màn`, cửa ải `80 × chương`, sao giữ tàu 15, sao kỹ năng 20, hoàn tất chương `100 + 40 × chương`.

DataStore schema 2 giữ số dư, kỷ lục, nâng cấp tài khoản, hướng dẫn và cài đặt cũ. Dữ liệu bổ sung gồm sở hữu/trang bị tàu, cấu hình Astra, từng bản ghi màn và danh sách lượt/chương đã thưởng. Mua và thưởng thực hiện trong giao dịch nguyên tử; cùng ID lượt chỉ ghi thưởng một lần, kể cả gọi đồng thời. Reset xóa tiến trình và tàu mua, giữ cài đặt. Ngôn ngữ tiếp tục do Android/AppCompat quản lý.

Không sửa ví, cấp tiền hoặc mở khóa giả trên điện thoại để kiểm tra. APK đã cài đè bằng `adb install -r`, không xóa dữ liệu.

## Kiểm chứng

- `:app:assembleDebug`: thành công, APK cài được trên thiết bị 25053RT47C.
- `:app:testDebugUnitTest`: **79 kiểm thử, 0 lỗi**.
- `:app:lintDebug`: **0 lỗi**, 544 cảnh báo. Phần lớn là tài nguyên được tra cứu động, kiểu chữ và gợi ý cấu hình; không coi đây là kết quả lint sạch cảnh báo.
- Mô phỏng tự động **100/100 màn thắng**, dùng Nova và nâng cấp tài khoản `Upgrades(2,3,3)`. Bot điều khiển ngang, nhặt vật phẩm, dùng xung và chọn nâng cấp bằng API chơi bình thường; không xóa chướng ngại vật trong bài mô phỏng này. Đây không phải kết quả chơi tay hoặc bằng chứng mọi sao đều đã đạt.
- Kiểm thử riêng: giá/sở hữu tàu; khóa màn; tổng thưởng 23.900; sao qua nhiều lượt; luyện tập; pause; kết quả đóng băng; xung không tắt cùng nút hai lần; Sentinel, Medic, Volt, Aurora, Guardian, Astra.
- Kiểm thử DataStore: migration schema 1, đóng/mở lại, reset, thưởng/mua đồng thời, lỗi ghi và thử lại, schema tương lai không bị ghi đè.
- Trên máy thật: cài đè thành công, mở giao diện tiếng Việt; đã xem màn chuẩn bị với mục tiêu và tàu được chọn. Ảnh lưu ở `artifacts/v2/current.png`. Không nhận đây là nghiệm thu thao tác tay toàn bộ 100 màn.

## Phạm vi nghiệm thu còn cần trước phát hành

Bản hiện tại là APK debug để chơi thử. Cần chơi tay kiểm tra nhịp từng màn, khả năng đạt ba sao, cảm giác điều khiển trên các tàu và đánh giá độ khó khi chưa nâng cấp tài khoản tối đa. Mô phỏng 100 màn không thay thế các bài này.

Các nội dung ngôn ngữ mới được dịch máy, cần người bản ngữ duyệt. Chưa đo FPS/bộ nhớ, chưa xác nhận giao diện RTL/font lớn trên đủ thiết bị, chưa chạy lại instrumentation trên điện thoại hoặc phát hành Play Store. Đồ họa vẫn là hình học Canvas, không phải bộ hình/hoạt cảnh vẽ riêng cho từng màn trong bản thiết kế.

## Tệp bàn giao

- APK: [MeteorDash-2.0-debug.apk](artifacts/v2/MeteorDash-2.0-debug.apk).
- Kết quả từng màn: [campaign-autopilot.txt](artifacts/v2/campaign-autopilot.txt).
- Số kiểm thử và SHA-256 APK: [verification.json](artifacts/v2/verification.json).
- Mã: `CampaignCatalog.kt`, `StageSession.kt`, `ShipCatalog.kt`, `CampaignScreens.kt`, `CampaignRenderer.kt`, cùng engine, ViewModel và repository đã cập nhật.
