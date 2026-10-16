# Meteor Dash — Bản nâng cấp 1.1: Risk & Reward

Ngày: 17/10/2026. Trạng thái: đã triển khai mã nguồn, build APK debug và cài lên điện thoại 25053RT47C. Tài liệu này mô tả bản nâng cấp thực tế, không thay thế tài liệu đặc tả MVP gốc.

## 1. Mục tiêu của bản nâng cấp

Bản đầu tập trung kéo tàu để né vật cản. Bản 1.1 thêm quyết định có rủi ro và phần thưởng: chủ động bay sát thiên thạch để tích combo, giữ hoặc dùng xung năng lượng để thoát hiểm, đọc cảnh báo đợt mưa và hoàn thành mục tiêu trong chuyến bay. Mức độ hấp dẫn và độ khó vẫn cần phản hồi sau khi chơi thử.

## 2. Những phần đã triển khai

### Combo né sát

- Một lần né sát được ghi khi tâm thiên thạch đi ngang độ cao tàu, cách tâm tàu lớn hơn tổng bán kính va chạm và không vượt quá tổng bán kính cộng 20 đơn vị.
- Mỗi thiên thạch chỉ được tính một lần. Va chạm, thời gian bất tử và thiên thạch bị phá bằng xung không được thưởng né sát.
- Né sát cho 20 điểm nhân hệ số thưởng và nạp 12 năng lượng.
- Chuỗi phải được nối trong 5 giây. Hết thời gian hoặc trúng giáp sẽ ngắt combo; điểm thưởng đã nhận được giữ lại.
- Combo 1–2: ×1; combo 3–5: ×2; combo 6–9: ×3. Combo hiện tại tối đa 9.
- Hệ số chỉ áp dụng cho điểm né sát, không nhân điểm thời gian, vật phẩm hoặc mục tiêu.
- HUD hiển thị combo, hệ số và số giây còn lại; có thông báo và âm thanh khi né sát.

### Kỹ năng xung năng lượng

- Có sẵn 100 năng lượng khi bắt đầu lượt. Bấm PHÁT XUNG để sử dụng khi đầy.
- Phá thiên thạch trong bán kính 180 đơn vị quanh tàu, tính cả bán kính thiên thạch; được bảo vệ tối thiểu 0,6 giây.
- Mỗi lần dùng tiêu thụ toàn bộ năng lượng. Không được điểm phá thiên thạch.
- Tự nạp 3 năng lượng/giây; né sát nạp 12; nhặt vật phẩm nạp 20; tối đa 100.
- Không dùng được khi đang hướng dẫn chưa bắt đầu, đếm ngược, tạm dừng hoặc đã thua. Yêu cầu lặp trước cùng một bước mô phỏng chỉ xử lý một lần.
- Nút và thanh năng lượng nằm ngoài vùng chơi, không che tàu. Xung có vòng lan và hạt hiệu ứng; giảm chuyển động dùng vòng cố định và ít hạt hơn.

### Mưa thiên thạch và vật cản bay chéo

- Đợt đầu ở giây 12; các đợt sau cách nhau 18 giây mô phỏng.
- Trước mỗi đợt có cảnh báo 1,6 giây. Vùng chơi chia ba làn; hai làn cam có dấu X, làn trống của đợt mưa dùng màu cyan và mũi tên.
- Mỗi làn bị đánh có tối đa ba thiên thạch, tổng tối đa sáu; vẫn kiểm tra giới hạn 80 đối tượng và bộ lọc khoảng trống.
- Tạm ngừng sinh thiên thạch thường trong giai đoạn cảnh báo và 4,5 giây sau cảnh báo để giữ nhịp dễ đọc.
- Thiên thạch cũ vẫn tồn tại, nên làn trống của đợt mưa không có nghĩa là bất tử hoặc không cần quan sát.
- Từ giây 20, một phần thiên thạch nhỏ/vừa có vận tốc ngang 22–42 đơn vị/giây trước hệ số tốc độ. Chúng đổi hướng khi tới mép; thiên thạch lớn vẫn rơi thẳng.
- Sinh vật cản, chọn làn và chuyển động ngẫu nhiên đều dùng bộ sinh seed của SpawnSystem.

### Mục tiêu trong mỗi lượt

| Mục tiêu | Thưởng |
|---|---:|
| Sống sót 30 giây | +100 điểm |
| Né sát 5 lần | +150 điểm |
| Nhặt 3 vật phẩm | +100 điểm |

Mỗi mục tiêu thưởng một lần trong lượt. Cả ba được theo dõi đồng thời; HUD hiện mục tiêu chưa hoàn thành đầu tiên và thanh tiến độ. Hoàn thành cả ba có nhãn 3/3, tổng thêm 350 điểm. Mục tiêu không phải nhiệm vụ ngày và không phụ thuộc mạng.

### Giao diện và phản hồi

- Nền tinh vân đổi tông theo khu vực; thêm vệt tốc độ khi combo cao.
- Thiên thạch bay chéo có vệt đuôi theo hướng chuyển động.
- Thông báo rõ khi né sát, nhặt đồ, trúng giáp, khiên chặn, phát xung, cảnh báo mưa và hoàn thành mục tiêu.
- Kết quả thêm số lần né sát, combo cao nhất, số lần phát xung, tổng điểm thưởng và mục tiêu hoàn thành.
- Hướng dẫn bổ sung đầy đủ các luật mới bằng tiếng Việt.
- Cắt hình trong biên Canvas để thiên thạch/hiệu ứng không vẽ tràn vào khu vực kỹ năng; thanh hệ thống dùng nền tối và biểu tượng sáng.

## 3. Điểm, tín dụng và dữ liệu

```text
Điểm cơ bản = floor(thời gian sống × 10) + số vật phẩm × 25
Tổng điểm = điểm cơ bản + điểm né sát + điểm mục tiêu
Tín dụng nhận được = floor(tổng điểm / 20)
```

Điểm thưởng nằm trong tổng điểm và chỉ được lưu khi kết thúc lượt bằng game over. Bỏ lượt không nhận thưởng. Công thức trừ tiền/nâng cấp và giao dịch chống thưởng trùng được giữ nguyên. Phiên bản dữ liệu vẫn là schema 1; tiền, kỷ lục, cài đặt và nâng cấp đã lưu được đọc lại. Thống kê combo/xung/mục tiêu là dữ liệu của lượt hiện tại, chưa có lịch sử riêng trên đĩa.

Tạm dừng đóng băng cả combo, năng lượng, cảnh báo, xung và thông báo. Không lưu lượt đang chơi qua process death; mở lại về trang chủ.

## 4. Cách chơi bản mới

1. Bắt đầu chuyến bay và kéo tàu ngang để né.
2. Ưu tiên sống sót, sau đó thử bay gần mép thiên thạch để nhận thưởng né sát.
3. Nối các lần né sát trong 5 giây để tăng hệ số.
4. Khi gặp tình huống khó, bấm PHÁT XUNG lúc đầy năng lượng; nhặt đồ và né sát để nạp lại nhanh hơn.
5. Khi có cảnh báo, đọc dấu X và mũi tên để chuyển làn; vẫn quan sát vật cản cũ.
6. Hoàn thành mục tiêu, kết thúc lượt và dùng tín dụng nâng cấp tàu.

## 5. Các file chính

- `app/src/main/java/com/example/nt_05_thienthach/game/GameEngine.kt`: cấu hình, combo, xung, wave, drift, mục tiêu và kết quả mở rộng.
- `app/src/main/java/com/example/nt_05_thienthach/GameViewModel.kt`: kết nối HUD và yêu cầu kỹ năng với fixed step.
- `app/src/main/java/com/example/nt_05_thienthach/ui/GameRenderer.kt`: cảnh báo làn, tinh vân, hiệu ứng và clip Canvas.
- `app/src/main/java/com/example/nt_05_thienthach/ui/MeteorDashApp.kt`: nút kỹ năng, mục tiêu, thống kê và hướng dẫn.
- `app/src/main/java/com/example/nt_05_thienthach/audio/AudioController.kt`: phản hồi âm thanh cho sự kiện mới.
- `app/src/test/java/com/example/nt_05_thienthach/UpgradeGameplayTest.kt`: 16 kiểm thử mới.

Phiên bản ứng dụng: versionName 1.1, versionCode 2. Android tối thiểu API 24; compile/target API 37.

## 6. Kiểm thử thực tế

Lệnh cuối:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Kết quả: BUILD SUCCESSFUL. Tổng 51 test đều đạt: 26 engine, 8 repository, 16 gameplay nâng cấp và 1 test mẫu cũ. Lint có 0 lỗi, 32 cảnh báo; chủ yếu dependency/version catalog, tài nguyên chưa dùng, khóa hướng và gợi ý plurals. Không dùng baseline để bỏ qua lỗi.

16 test mới kiểm tra thưởng né sát một lần, không thưởng va chạm/đi xa, ngưỡng combo và giới hạn, hết combo/trúng giáp, phạm vi xung, không thưởng phá vật cản, chặn yêu cầu lặp, hủy yêu cầu khi pause, nạp năng lượng, wave cảnh báo trước khi sinh, làn trống/giới hạn đối tượng, drift có seed/bật mép, mỗi mục tiêu thưởng một lần, kết quả và đóng băng các timer mới khi pause.

Đã cài bằng `adb install -r` và mở thành công trên điện thoại 25053RT47C. Đã quan sát trực tiếp HUD combo/năng lượng, thông báo hoàn thành mục tiêu, nhãn 3/3; thử nút tạm dừng và Back về pause. Đã kiểm tra ảnh trang chủ cuối sau khi sửa màu thanh hệ thống. Chưa kiểm thử đầy đủ 20 lượt, nhiều tỷ lệ màn hình/font lớn, chưa đo FPS/bộ nhớ hoặc đánh giá âm thanh/rung bằng nghe/cảm nhận. Chưa khẳng định độ khó đã cân bằng.

- [Ảnh trang chủ bản cuối](docs/v1.1/home.png)
- [Ảnh gameplay](docs/v1.1/gameplay.png)
- [Ảnh hoàn thành mục tiêu](docs/v1.1/missions-complete.png)
- [Ảnh tạm dừng](docs/v1.1/pause.png)

Ảnh gameplay/mục tiêu/pause được chụp trong phiên kiểm tra trước bản sửa cắt Canvas và thanh hệ thống; các cơ chế gameplay không thay đổi ở bản sửa cuối.

APK: [app-debug.apk](app/build/outputs/apk/debug/app-debug.apk). Log: [upgrade-build.log](upgrade-build.log). Đây là APK debug dùng để thử, chưa ký release/phát hành.

## 7. Đánh giá tiếp theo

Cần phản hồi sau khi chơi thử về độ rõ cảnh báo, tần suất mưa, tốc độ nạp xung, độ khó né sát và mức thưởng. Các giá trị tập trung trong GameConfig để chỉnh sau playtest. Kỷ lục cũ vẫn được giữ; do điểm thưởng mới, điểm số phiên bản 1.1 có thể tăng nhanh hơn bản đầu.
