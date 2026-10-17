# Meteor Dash / Né Thiên Thạch

Game Android native offline, Kotlin + Jetpack Compose + Canvas. Portrait, minSdk 24 (giữ cấu hình dự án hiện có), compileSdk/targetSdk 37. Không có tài khoản, quảng cáo hoặc quyền Internet.

## Build

Dùng Android Studio với SDK platform 37 và JDK đi kèm. Trên PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Mở project, chờ Gradle sync, chọn thiết bị API 24 trở lên rồi Run. APK debug dùng để review, chưa phải bản ký release.

Công cụ: Gradle wrapper 9.4.1, AGP 9.2.1, Kotlin/Compose compiler 2.2.10, Compose BOM 2026.09.00, Activity Compose 1.13.0, Lifecycle 2.10.0, Preferences DataStore 1.2.1, JUnit 4.13.2. Kotlin dùng hỗ trợ tích hợp của AGP 9; không áp dụng kotlin-android plugin.

## Chức năng

- Sinh tồn vô tận; kéo ngang tương đối, hỗ trợ một ngón, không nhảy tàu khi chạm.
- Ba loại thiên thạch, độ khó mỗi 20 giây, kiểm tra lối trống 60 đơn vị.
- Khiên, hồi giáp, làm chậm; hitbox tròn và swept collision cho kéo nhanh.
- Hướng dẫn lần đầu, đếm ngược, tạm dừng/tiếp tục, xác nhận bỏ lượt.
- Kết quả, kỷ lục, tín dụng, ba nhánh nâng cấp và giao dịch DataStore nguyên tử.
- Nhạc tổng hợp, hiệu ứng âm thanh Android, rung, giảm chuyển động.
- Tự pause khi ra nền, mất focus cửa sổ/âm thanh; trở lại không tự chạy.
- Menu cuộn được, vùng an toàn hệ thống, nút tối thiểu 48dp, tài nguyên tiếng Việt.

## Kiến trúc

- `game/GameEngine.kt`: cấu hình, model, spawn có seed, collision, máy trạng thái, fixed step 60 Hz; không import Android. Giới hạn frame delta 100 ms / sáu bước.
- `data/ProgressRepository.kt`: Preferences DataStore, kiểm tra schema, clamp dữ liệu, corruption handler; giao dịch thưởng/mua; reset giữ cài đặt.
- `GameViewModel.kt`: một chủ sở hữu engine trên main thread, HUD chỉ cập nhật khi giá trị thay đổi; frame tick chỉ được đọc trong draw; không ghi disk trong render loop.
- `ui/`: menu Compose, Canvas và chuyển đổi tọa độ theo vùng logic 360 x 800.
- `audio/AudioController.kt`: nhạc PCM tổng hợp và audio focus, hiệu ứng một lần theo sự kiện.

Lưu thưởng được tuần tự hóa, khóa lượt tiếp theo đến khi lưu thành công. `lastRewardedRunId` chống lặp cho lượt mới nhất; không hỗ trợ nhiều kết quả chờ lưu. Lỗi ghi giữ kết quả trong bộ nhớ, có nút thử lại. Dữ liệu schema mới hơn bị từ chối thay vì xóa. Schema 1 dùng giá trị mặc định khi thiếu key. Không lưu lượt đang chơi; process death trở về trang chủ, không thưởng lượt dở. Android backup đã tắt (`allowBackup=false`); không có cloud save.

## Asset / quyền sử dụng

Hình tàu, sao, thiên thạch, vật phẩm, hạt và màn hình đều vẽ bằng mã Canvas trong dự án. Nhạc 8 giây được tạo bằng sóng sin trong AudioController; không có bản thu hay asset bên thứ ba. Hiệu ứng dùng Android ToneGenerator. Font hệ thống. Launcher là vector tàu riêng của game, tương thích API 24; cần duyệt tên/logo chính thức trước phát hành. Thư viện AndroidX theo Apache-2.0; giữ thông tin giấy phép khi phân phối.

## QA cần thiết trước phát hành

Unit test bao phủ điểm/thưởng, mốc độ khó, swept collision, bất tử, hiệu ứng, pause, countdown, spawn seed/giới hạn và giao dịch nâng cấp. Kết quả build và kiểm thử thực tế ghi ở `QA.md`.

Cần playtest cân bằng và đo FPS/bộ nhớ trên thiết bị thật; không mặc định tuyên bố 60 FPS. Kiểm tra tối thiểu ba tỷ lệ màn hình/font lớn, đa chạm, máy bay, audio focus, 20 lượt liên tục, process death, lỗi ghi và corruption. Đồ họa hình học là asset MVP; chưa có bản ký release hoặc phát hành Play Store.


## Bản 1.1

Đã thêm combo né sát, xung năng lượng chủ động, đợt mưa có cảnh báo, thiên thạch bay chéo và ba mục tiêu trong mỗi lượt. Hướng dẫn, thông số và kết quả kiểm thử mới nằm trong [METEOR_DASH_UPGRADE_v1.1.md](METEOR_DASH_UPGRADE_v1.1.md). `QA.md` là báo cáo của bản MVP ban đầu; tham khảo tài liệu 1.1 cho trạng thái mới nhất.

## Bản 1.2

Thêm ba lựa chọn chiến thuật trong lượt tại mốc 45/90/135 giây, hướng dẫn theo hành động có thể chơi lại, mục tiêu và gợi ý ở màn kết quả. Xem [METEOR_DASH_UPGRADE_v1.2.md](METEOR_DASH_UPGRADE_v1.2.md) để biết lợi ích/đánh đổi và kết quả xác minh mới nhất (59 test đạt).

## Bản 1.3 — 20 ngôn ngữ

Chọn ngôn ngữ từ nút 🌐 trên trang chủ hoặc Cài đặt. Tự theo thiết bị hoặc chọn thủ công; lưu qua lần mở app, hỗ trợ RTL, dùng offline và giữ tiến trình. Xem [METEOR_DASH_UPGRADE_v1.3.md](METEOR_DASH_UPGRADE_v1.3.md) để biết danh sách 20 thị trường, cách cập nhật bản dịch và xác minh mới nhất (63 unit test đạt, kiểm tra giao diện 20 locale trên điện thoại).
