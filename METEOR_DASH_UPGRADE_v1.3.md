# Meteor Dash 1.3 — 20 ngôn ngữ

Ngày 17/10/2026. versionName 1.3, versionCode 4. APK debug đã cài lên điện thoại 25053RT47C.

## Sử dụng

Bấm **🌐 Language / Ngôn ngữ** ở đầu trang chủ hoặc **Cài đặt → Ngôn ngữ**. Danh sách có thể cuộn. Chọn **Theo ngôn ngữ thiết bị** hoặc một trong 20 mục. Thay đổi có hiệu lực ngay; Android tự lưu và giữ sau khi mở lại app. Khi ngôn ngữ thiết bị chưa được hỗ trợ, giao diện dùng tiếng Anh.

| Quốc gia ưu tiên | Ngôn ngữ | Mã |
|---|---|---|
| Việt Nam | Tiếng Việt | vi |
| Mỹ | English | en |
| Trung Quốc | 简体中文 (giản thể) | zh |
| Nhật Bản | 日本語 | ja |
| Hàn Quốc | 한국어 | ko |
| Brazil | Português | pt |
| Mexico | Español | es |
| Ấn Độ | हिन्दी | hi |
| Indonesia | Bahasa Indonesia | id |
| Thái Lan | ไทย | th |
| Nga | Русский | ru |
| Đức | Deutsch | de |
| Pháp | Français | fr |
| Thổ Nhĩ Kỳ | Türkçe | tr |
| Saudi Arabia | العربية | ar |
| Italy | Italiano | it |
| Ba Lan | Polski | pl |
| Philippines | Filipino | fil |
| Malaysia | Bahasa Melayu | ms |
| Pakistan | اردو | ur |

Đây là danh sách thị trường ưu tiên triển khai, không phải bảng xếp hạng 20 nước thích riêng game né thiên thạch nhất. Chưa có dữ liệu người chơi của game để kết luận điều đó. Tham khảo xu hướng tải game và kết hợp độ phủ địa lý/ngôn ngữ: [Sensor Tower, tháng 1/2025](https://sensortower.com/blog/top-10-worldwide-mobile-games-by-revenue-and-downloads-in-january-2025), [AppMagic Mobile Market Landscape 2026](https://appmagic.rocks/files/view/upload/Reports/EN_MobileMarkeLandscape2026.pdf). Một quốc gia có thể dùng nhiều ngôn ngữ; người chơi được chọn bất kỳ mục nào, không bị khóa theo quốc gia.

## Phạm vi

- Dịch đầy đủ 155 chuỗi giao diện cho mỗi ngôn ngữ: trang chủ, gameplay/HUD, pause, kết quả, xưởng, cài đặt, xác nhận, lỗi lưu, hướng dẫn và nâng cấp chiến thuật.
- Tên METEOR DASH giữ nguyên thương hiệu; 2 chuỗi định dạng số/thời gian dùng chung. Danh mục gốc có 158 khóa.
- Tên các nâng cấp chiến thuật đã chuyển khỏi chuỗi tiếng Việt viết trực tiếp trong Kotlin.
- Số thập phân trong mô tả nâng cấp dùng locale đang hiển thị.
- Ả Rập và Urdu có bố cục RTL, hướng nút quay lại phù hợp, khoảng cách chữ và độ rộng tiêu đề được điều chỉnh.
- Toàn bộ bản dịch đóng gói trong APK, không thêm quyền mạng. Không gửi dữ liệu người chơi đi dịch.
- Giữ schema và DataStore của tiến trình. Cài đè không xóa tiền, kỷ lục, nâng cấp. Reset tiến trình giữ ngôn ngữ như các thiết lập khác.

## Triển khai

Dùng AppCompatActivity + AppCompatDelegate.setApplicationLocales; autoStoreLocales cho Android trước 13, localeConfig và cơ chế per-app language trên Android 13+. Theo [Android Developers](https://developer.android.com/guide/topics/resources/app-languages). Không tạo kho lưu ngôn ngữ song song với hệ thống.

- `app/src/main/java/com/example/nt_05_thienthach/data/AppLanguages.kt`: 20 lựa chọn và ánh xạ biến thể quốc gia/alias.
- `app/src/main/java/com/example/nt_05_thienthach/ui/MeteorDashApp.kt`: hộp chọn, đổi ngôn ngữ, dịch tên kỹ năng và chỉnh RTL.
- `app/src/main/res/values*/strings.xml`: tài nguyên Android. `values/` là tiếng Anh dự phòng; `values-vi/` là tiếng Việt.
- `tools/localizations.json`: nguồn bản dịch theo khóa.
- `tools/build_localizations.py`: kiểm tra đủ 20 bộ, đủ khóa, không chuỗi rỗng, đúng số lượng/kiểu tham số rồi tạo XML. Chạy bằng Python từ repo: `python tools/build_localizations.py`.

## Xác minh

- Build cuối: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain` — BUILD SUCCESSFUL.
- 63 unit test đạt, 0 lỗi: 59 test cũ và 4 test ngôn ngữ (20 mục duy nhất, locale vùng, alias id/fil, hệ thống/ngôn ngữ không hỗ trợ).
- Lint: 0 lỗi, 80 cảnh báo. Chủ yếu gợi ý plurals, typography, dependency và tài nguyên mẫu; có cảnh báo localeConfig API 33 và alias Indonesian in/id. Không dùng baseline để bỏ qua.
- Kiểm tra nguồn và XML sinh ra: đủ 20 bộ, đúng khóa/tham số. Brand chỉ khai báo ở tài nguyên mặc định để tránh dịch thương hiệu.
- Đã cài APK chính và kiểm tra đổi đủ 20 ngôn ngữ bằng cơ chế per-app locale trên điện thoại. Mỗi cây giao diện có ít nhất 3 chuỗi dịch khớp chính xác; có lúc màn hình cuộn hoặc đang mở hộp chọn nên không đòi tiêu đề luôn hiện. Bằng chứng: `docs/v1.3/locale-ui-report.json` và các `*-ui.xml`.
- Tệp tiến trình trước/sau thử 20 locale có cùng SHA-256; không sửa tiền, kỷ lục hoặc nâng cấp trong phép thử này.
- Thử nút 🌐 thật, chọn tiếng Nhật, đóng tiến trình rồi mở lại: vẫn tiếng Nhật. Xác nhận trong `docs/v1.3/picker-persistence-report.json`.
- Đã xem ảnh trang chủ Nhật, hộp chọn, màn kết quả Việt và bố cục RTL; sửa lỗi tiêu đề RTL co hẹp và xem lại bản cuối. Ảnh: `language-picker.png`, `restarted-ja.png`, `final-ar.png`, `final-ur.png`.
- Ngôn ngữ ban đầu (theo thiết bị) đã được khôi phục sau kiểm tra.

## Giới hạn kiểm thử

Đã biên dịch 4 instrumentation test về tài nguyên/định dạng/RTL/fallback nhưng chưa chạy được: điện thoại từ chối cài gói test riêng với INSTALL_FAILED_USER_RESTRICTED / Install canceled by user. Không thay đổi bảo mật thiết bị để vượt qua. Kiểm tra UI thực tế ở trên chạy trực tiếp với APK chính.

Chưa có người bản ngữ rà soát bản dịch, chưa kiểm tra toàn bộ màn hình ở mọi cỡ chữ/thiết bị, chưa kiểm chứng lưu lựa chọn trên Android 7–12 bằng thiết bị thật. APK là bản debug để thử, chưa ký release/phát hành.

APK: [app-debug.apk](app/build/outputs/apk/debug/app-debug.apk).
