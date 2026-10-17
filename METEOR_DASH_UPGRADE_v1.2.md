# Meteor Dash 1.2 — Chọn cách bay

Ngày 17/10/2026. versionCode 3, versionName 1.2.

## Nội dung

- Mốc 45, 90 và 135 giây mô phỏng mở lựa chọn nâng cấp, tạm dừng toàn bộ thế giới. Mỗi loại chọn tối đa một lần, hiệu lực trong lượt; không tiêu tín dụng hay thay đổi nâng cấp vĩnh viễn. Sau khi chọn có đếm ngược 3 giây.
- Tụ khuếch đại: bán kính xung 180 → 250; tự nạp 3 → 2,1 năng lượng/giây. Nhặt đồ và né sát vẫn nạp như trước.
- Phi công mạo hiểm: điểm né sát gấp đôi; giáp tối đa giảm 1, giáp hiện tại bị hạ về mức mới nếu vượt mức. Hồi giáp tôn trọng mức mới.
- Tập trung: thời gian nối combo 5 → 8 giây; vật phẩm làm chậm nhặt sau khi chọn chỉ còn 80% thời lượng bình thường. Hiệu ứng đang có giữ thời hạn cũ.
- Tạm dừng/ra nền ở màn chọn sẽ trở lại màn chọn, không bỏ qua lựa chọn. Đã chọn thì loại đó biến mất khỏi danh sách.
- HUD hiển thị các nâng cấp đã chọn; vòng xung dùng bán kính thực tế mới.
- Hướng dẫn theo hành động: kéo tàu, nhặt vật phẩm, phát xung, né sát. Tiến độ dựa trên hành động đã thực hiện trong lượt, kể cả làm trước khi tới bước đó. Có nút ẩn; chỉ lưu hoàn thành sau đủ các hành động hoặc người chơi bấm đã hiểu. Lượt chưa hoàn tất có thể hướng dẫn lại lần sau.
- Trang Hướng dẫn có nút chơi với hướng dẫn từng bước, dùng được cả khi đã hoàn thành hướng dẫn trước đó.
- Màn kết quả thêm gợi ý theo năng lượng khi kết thúc, thời gian sống và số lần né sát; liệt kê nâng cấp trong lượt; báo số tín dụng còn thiếu để mua cấp nâng tiếp theo có giá thấp nhất, kèm lối vào xưởng tàu. Không suy đoán nguyên nhân tử vong.

## Dữ liệu

Giữ schema 1 và cơ chế thưởng/mua DataStore hiện tại. Không thêm quyền mạng. Nâng cấp chiến thuật và tiến độ từng bước chỉ nằm trong lượt; không khôi phục lượt dở qua process death. Tiền, kỷ lục, cài đặt và nâng cấp tàu cũ được giữ khi cài đè.

## Xác minh

Lệnh: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`, JAVA_HOME là JBR Android Studio.

- BUILD SUCCESSFUL ở lần kiểm tra cuối.
- 59 test đạt, 0 failure/error: 26 engine, 8 repository, 16 gameplay 1.1, 8 lựa chọn chiến thuật 1.2, 1 test mẫu.
- 8 test mới kiểm tra mốc 45 giây/đóng băng, chặn chọn sai pha/chọn lặp, đếm ngược, pause tại màn chọn, bán kính xung và tốc độ nạp, điểm/giáp/hồi giáp, combo/làm chậm, ba mốc và giới hạn/không rò sang lượt mới, giữ công thức thưởng và nâng cấp vĩnh viễn.
- Lint: 0 lỗi, 34 cảnh báo; không thêm baseline để bỏ qua.
- Cài đè APK và mở trên điện thoại 25053RT47C thành công. Đã xem ảnh gameplay thực tế trong docs/v1.2/gameplay.png; ảnh lấy từ bản 1.2 trước bổ sung nút chơi lại hướng dẫn, cơ chế chiến thuật không đổi.
- Chưa xác minh trực quan đầy đủ màn chọn tại cả ba mốc hoặc toàn bộ các bước hướng dẫn trên máy thật; kiểm thử tự động xác minh cơ chế engine. Chưa đo FPS/bộ nhớ, font lớn hoặc chơi 20 lượt để cân bằng.

APK debug: app/build/outputs/apk/debug/app-debug.apk. Chưa ký release hoặc phát hành.
