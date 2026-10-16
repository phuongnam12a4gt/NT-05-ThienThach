# Báo cáo kiểm thử — 17/10/2026

## Kết quả thực tế

Lệnh chạy trên Windows với JBR của Android Studio:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

- BUILD SUCCESSFUL, lần cuối 13 giây.
- GameEngineTest: 26 test, 0 failure/error.
- ProgressRepositoryTest: 8 test, 0 failure/error.
- ExampleUnitTest của project gốc: 1 test, 0 failure/error.
- Tổng: 35 test; 34 test mới có ý nghĩa cho gameplay/storage.
- Lint: 0 lỗi, 27 cảnh báo. Chủ yếu phiên bản dependency mới hơn, dùng version catalog, khóa hướng màn hình, tài nguyên mẫu cũ chưa dùng và gợi ý plurals. Tiếng Việt không đổi dạng số nhiều. Không dùng baseline để bỏ qua lỗi.
- APK debug: `app/build/outputs/apk/debug/app-debug.apk`, 12.777.163 byte tại lần build cuối.
- Chi tiết: `build-verification.log`, `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/lint-results-debug.html`.

## Đã kiểm tra tự động

Điểm và làm tròn thưởng; mốc độ khó/cap; sai số fixed step; swept collision tại biên; clamp kéo tàu; countdown/tutorial/pause; nhiều va chạm trong cùng bước; khiên ưu tiên trước va chạm; bất tử không tiêu thụ khiên; hồi giáp; nhặt lặp; hết hiệu ứng ở đúng biên; vận tốc làm chậm; giới hạn delta; kết thúc một lần và đóng băng kết quả; hai giây đầu không có thiên thạch; seed/tỷ lệ loại/giới hạn spawn/lối trống; không sinh sửa chữa khi đầy giáp.

Giao dịch thưởng idempotent, mua đủ/thiếu tiền/tối đa; DataStore thật qua tệp tạm: mặc định khi thiếu key, clamp số không hợp lệ, từ chối schema tương lai không xóa dữ liệu, 30 giao dịch thưởng đồng thời, 20 lượt mua đồng thời không chi vượt số dư, reset giữ settings, đóng/mở lại tệp; store giả lập lỗi ghi rồi thử lại không thưởng hai lần.

## Chưa kiểm chứng trên Android đang chạy

`adb devices -l` không có thiết bị; `emulator -list-avds` không có AVD và SDK chưa có system image. Vì vậy chưa chạy instrumented test hoặc kiểm tra giao diện thực tế. Chưa xác nhận audio focus, rung/nhạc nghe thực tế, đa chạm/cancel hệ thống, font lớn, ba tỷ lệ màn hình, FPS, memory, 20 lượt liên tục, máy bay, process death và corruption handler trên thiết bị. Không tuyên bố game đạt 60 FPS hay hoàn thành nghiệm thu thiết bị.

## Quyết định triển khai

- Giữ minSdk 24 mà project hiện có; đặc tả chỉ đề xuất 26, không bắt buộc. Target/compile 37 giữ nguyên.
- Chuyển entry point GameActivity/C++ sang ComponentActivity/Kotlin/Compose; CMake và thư viện native không còn tham gia build. Mã C++ mẫu cũ được giữ lại trên đĩa.
- Áp dụng toàn bộ phạm vi MVP; mục prompt gói trong tài liệu là mẫu tham khảo.
- Dùng đồ họa vector/Canvas và nhạc tự tổng hợp để có bản chơi offline, không phụ thuộc tải asset.
- Tắt backup/cloud và device transfer bằng manifest/XML. Tiến trình chỉ trên thiết bị.
- Runtime không lưu giữa lượt; process death bỏ lượt dở, lần mở tiếp theo về trang chủ.

## Cài và chơi

1. Cài APK debug lên điện thoại API 24 trở lên hoặc chạy bằng Android Studio.
2. Chọn Bắt đầu chuyến bay, chạm rồi kéo ngang để hoàn thành hướng dẫn lần đầu.
3. Né thiên thạch; nhặt khiên, sửa chữa và làm chậm. Hết giáp mở kết quả và lưu tín dụng.
4. Dùng tín dụng nâng cấp tàu tại xưởng. Các lượt sau bắt đầu bằng đếm ngược ba giây.

Cần playtest cân bằng, duyệt thương hiệu và ký release trước khi phát hành.
