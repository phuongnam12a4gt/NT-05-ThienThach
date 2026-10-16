# NÉ THIÊN THẠCH — ĐẶC TẢ GAME ANDROID NATIVE

- Tên tiếng Anh: Meteor Dash.
- Phiên bản tài liệu: 1.0.
- Ngày: 17/10/2026.
- Trạng thái: đặc tả đề xuất để triển khai MVP bằng AI; chưa phải sản phẩm đã được xây dựng hoặc kiểm thử.
- Nền tảng: Android native, Kotlin, màn hình dọc, offline.
- Mọi thông số cân bằng trong tài liệu là giá trị khởi đầu, cần playtest trước khi phát hành.

## 1. Tầm nhìn và phạm vi

Game arcade sinh tồn 2D. Người chơi kéo tàu vũ trụ theo chiều ngang để né thiên thạch, nhặt vật phẩm, sống lâu nhất có thể và vượt kỷ lục cá nhân. Tiền kiếm được sau mỗi lượt dùng để nâng cấp tàu.

Trải nghiệm mong muốn: hiểu điều khiển trong 10 giây, bắt đầu lượt mới trong tối đa hai thao tác từ trang chủ, nhận biết rõ nguồn sát thương và có động lực chơi lại. Một lượt của người mới dự kiến kéo dài 1–3 phút; game không giới hạn thời gian.

### 1.1 Trong MVP

- Một chế độ sinh tồn vô tận.
- Một tàu có ba nhánh nâng cấp.
- Ba loại thiên thạch và ba loại vật phẩm.
- Trang chủ, gameplay, tạm dừng, kết quả, nâng cấp, cài đặt, hướng dẫn.
- Điểm, kỷ lục, tiền, âm thanh, rung và lưu tiến trình offline.
- Giao diện tiếng Việt; chuỗi văn bản tách vào tài nguyên.

### 1.2 Ngoài MVP

Không triển khai tài khoản, backend, multiplayer, bảng xếp hạng online, cloud save, quảng cáo, mua bằng tiền thật, nhiệm vụ ngày, boss, đạn, phá thiên thạch, skin trả phí hoặc thông báo đẩy. Các mục này chỉ được thêm khi có yêu cầu mới.

## 2. Vòng lặp và mục tiêu

1. Từ trang chủ chọn Chơi.
2. Hoàn thành hướng dẫn lần đầu hoặc đếm ngược trước lượt.
3. Kéo ngang né thiên thạch và nhặt vật phẩm.
4. Điểm tăng theo thời gian sống và số vật phẩm.
5. Độ khó tăng mỗi 20 giây gameplay.
6. Giáp về 0 thì kết thúc ngay.
7. Ghi kết quả, cộng thưởng đúng một lần, hiển thị màn kết quả.
8. Chơi lại hoặc về trang chủ mua nâng cấp.

Không có màn thắng. Kỷ lục cá nhân là mục tiêu dài hạn. Bỏ lượt giữa chừng không nhận thưởng và không ghi kỷ lục của lượt đó.

## 3. Không gian chơi và tọa độ

- Không gian logic: rộng 360, cao 800 đơn vị; gốc tọa độ ở góc trên trái.
- Trục X tăng sang phải, Y tăng xuống dưới.
- Vùng logic nằm trong vùng màn hình an toàn sau khi trừ system insets.
- Scale đồng nhất: min(availableWidth / 360, availableHeight / 800).
- Căn giữa vùng logic; phần dư vẽ nền, không kéo giãn đối tượng.
- Chuyển vị trí chạm sang tọa độ logic bằng cách trừ offset rồi chia scale.
- Chạm trong phần nền dư không bắt đầu điều khiển.
- HUD nằm trong vùng logic và không bị camera khuyết hoặc thanh điều hướng che.
- Khóa màn hình dọc; không thiết kế chế độ ngang trong MVP.

## 4. Tàu và điều khiển

### 4.1 Thuộc tính cơ bản

| Thuộc tính | Giá trị |
|---|---:|
| Tâm khi bắt đầu | (180, 680) |
| Kích thước hình vẽ gợi ý | 32 × 40 |
| Bán kính hitbox | 12 |
| Giáp cơ bản | 3 |
| Sát thương một va chạm | 1 |
| Bất tử sau trúng đòn | 1,2 giây |
| Khoảng giới hạn tâm X | 20–340 |

Tàu chỉ di chuyển ngang; Y giữ ở 680. Hình vẽ có thể lớn hơn hitbox để cảm giác né dễ chịu. Chuyển động kéo không có quán tính trong MVP.

### 4.2 Quy tắc chạm

- Khi pointer đầu tiên chạm vùng chơi, lưu X ngón tay và X tàu làm mốc.
- Trong khi kéo: targetX = startShipX + currentPointerX - startPointerX.
- Clamp targetX trong khoảng 20–340; áp dụng ở bước mô phỏng kế tiếp.
- Tàu không nhảy tới vị trí ngón tay lúc chạm xuống.
- Bỏ qua các ngón bổ sung. Khi ngón đang điều khiển nhấc lên, không tự chuyển sang ngón khác.
- UP, CANCEL, mất foreground hoặc tạm dừng đều xóa pointer đang theo dõi.
- Nút tạm dừng xử lý chạm riêng, không điều khiển tàu.
- Sau khi tạm dừng rồi tiếp tục, cần một lần chạm mới để kéo.

### 4.3 Phản hồi sát thương

Tàu nhấp nháy trong thời gian bất tử. Va chạm hợp lệ phát âm thanh, rung ngắn nếu được bật và hiệu ứng tại vị trí trúng. Hiển thị mất giáp ngay trong cùng lần cập nhật UI kế tiếp. Không chỉ dùng màu sắc để báo sát thương.

## 5. Thiên thạch

| Loại | Bán kính | Tốc độ cơ bản | Hành vi |
|---|---:|---:|---|
| Nhỏ | 12 | 220 đơn vị/giây | Rơi thẳng |
| Vừa | 22 | 170 đơn vị/giây | Rơi thẳng |
| Lớn | 34 | 125 đơn vị/giây | Rơi thẳng |

- Mọi loại gây một sát thương.
- Sinh với tâm Y = -radius, tâm X trong [radius, 360 - radius].
- Vận tốc thực = tốc độ cơ bản × hệ số độ khó × hệ số làm chậm hiện tại.
- Khi Y - radius > 800 thì xóa khỏi thế giới.
- Thiên thạch đã va chạm tàu bị xóa, kể cả khi tàu đang bất tử.
- Không thưởng điểm riêng khi né một thiên thạch.
- Xoay hình thiên thạch chỉ là hiệu ứng hình ảnh, không đổi hitbox.

## 6. Độ khó và sinh vật cản

Đặt t là thời gian mô phỏng trong trạng thái RUNNING, tính bằng giây:

```text
difficultyLevel = floor(t / 20)
speedMultiplier = min(1 + difficultyLevel × 0.08, 2.2)
spawnIntervalSeconds = max(0.90 - difficultyLevel × 0.05, 0.30)
```

| Thời gian | Nhỏ | Vừa | Lớn |
|---|---:|---:|---:|
| 0 đến dưới 20 giây | 100% | 0% | 0% |
| 20 đến dưới 60 giây | 65% | 35% | 0% |
| Từ 60 giây | 50% | 35% | 15% |

- Hai giây đầu không sinh thiên thạch. Lần thử sinh đầu tại t = 2 giây.
- Mỗi lần thử sinh tối đa một đối tượng.
- Sau mỗi lần thử, lên lịch lần tiếp theo bằng khoảng sinh ứng với độ khó hiện tại; bỏ lần sinh không tạo dồn đối tượng bù.
- Tối đa 80 thiên thạch hoạt động. Đạt giới hạn thì bỏ lần sinh.
- Dùng bộ sinh số ngẫu nhiên có thể truyền seed; không dùng random rải rác ngoài SpawnSystem.

### 6.1 Bộ lọc bố trí

Tại một lần sinh, xét thiên thạch ứng viên và những thiên thạch có tâm Y trong khoảng -34 đến 140. Lấy các khoảng X bị chiếm bởi hitbox; gộp các khoảng giao nhau. Chỉ nhận ứng viên nếu phần còn trống trong [0, 360] có ít nhất một khoảng liên tục rộng 60 đơn vị.

Thử tối đa tám X ứng viên; nếu tất cả bị từ chối thì bỏ lần sinh. Đây là bộ lọc cơ bản, không bảo đảm toàn bộ tương lai luôn né được. Cần playtest ở độ khó cao và cải thiện bộ lọc theo quỹ đạo nếu xuất hiện tình huống bất khả thi.

## 7. Vật phẩm

Tất cả có bán kính nhặt 12, rơi thẳng với tốc độ 140 đơn vị/giây, sinh ngoài phía trên tương tự thiên thạch. Tối đa hai vật phẩm đang rơi.

| Loại | Hiệu lực cơ bản | Nhặt lặp |
|---|---|---|
| Khiên | Chặn một va chạm; hết hạn sau 8 giây | Đặt lại thời hạn đầy đủ, không tích số lần chặn |
| Sửa chữa | Hồi một giáp | Clamp theo giáp tối đa |
| Làm chậm | Thiên thạch còn 60% tốc độ trong 5 giây | Đặt lại thời hạn đầy đủ, không nhân chồng |

- Lần thử sinh đầu tại t = 10 giây; các lần sau cách nhau ngẫu nhiên 10–15 giây.
- Tỷ lệ khiên/sửa chữa/làm chậm: 40%/25%/35%.
- Nếu đầy giáp tại thời điểm sinh, bỏ sửa chữa và chuẩn hóa hai tỷ lệ còn lại thành 40/75 và 35/75.
- Nếu đạt giới hạn vật phẩm thì bỏ lần sinh và vẫn lên lịch lần tiếp theo.
- Vật phẩm nhặt được bị xóa ngay và tăng collectedItems một lần.
- Sửa chữa vẫn bị tiêu thụ và tính điểm nếu tàu đã đầy giáp lúc nhặt.
- Làm chậm áp dụng lên cả thiên thạch đang có và sinh mới; không làm chậm đồng hồ điểm, tàu hoặc vật phẩm.
- Khi khiên chặn đòn, tiêu thụ khiên, xóa thiên thạch và bật bất tử 1,2 giây.
- Trong thời gian bất tử, va chạm không tiêu thụ khiên.
- Đồng hồ hiệu ứng dùng thời gian mô phỏng, đóng băng khi pause.

## 8. Mô phỏng và va chạm

Mô phỏng bước cố định 1/60 giây, render theo nhịp khung hình. Dùng thời gian đơn điệu, không dùng giờ hệ thống. Giới hạn delta nhận mỗi khung hình ở 0,10 giây và tối đa sáu bước bù; bỏ phần tích lũy quá giới hạn để tránh vòng lặp bù vô tận. Khi thiết bị quá chậm, thời gian game có thể chạy chậm hơn thời gian thực; điểm vẫn dựa trên thời gian mô phỏng.

Thứ tự mỗi bước:

1. Nhận trạng thái điều khiển và cập nhật tàu.
2. Giảm thời gian còn lại của hiệu ứng, clamp ở 0.
3. Cập nhật độ khó, lịch sinh, sinh đối tượng và vị trí đối tượng.
4. Xử lý vật phẩm chạm tàu.
5. Xử lý thiên thạch chạm tàu.
6. Xóa đối tượng đã tiêu thụ hoặc ra khỏi vùng chơi.
7. Tăng thời gian sống, tính điểm và kiểm tra kết thúc.

Hai hình tròn va chạm nếu distanceSquared <= (radiusA + radiusB)^2. Với tàu được kéo nhanh, kiểm tra thêm giao cắt giữa đoạn di chuyển của tâm tàu trong bước và hình tròn thiên thạch có bán kính mở rộng bằng tổng bán kính, tránh kéo xuyên vật cản. Duyệt đối tượng theo ID tăng dần để kết quả tái hiện ổn định.

Khi nhận sát thương, đặt bất tử ngay trước khi xử lý thiên thạch tiếp theo. Nếu giáp về 0, dừng xử lý sát thương còn lại và kết thúc lượt trong bước đó. Nếu nhặt vật phẩm và chạm thiên thạch cùng bước, hiệu ứng vật phẩm được áp dụng trước. Giáp không âm.

## 9. Điểm, tiền và kinh tế

```text
score = floor(survivalTimeSeconds × 10) + collectedItems × 25
earnedCredits = floor(finalScore / 20)
```

- Điểm hiển thị là số nguyên không âm.
- Không tăng điểm khi READY, COUNTDOWN, PAUSED hoặc GAME_OVER.
- Bằng kỷ lục cũ không được coi là kỷ lục mới.
- totalRuns chỉ tăng đối với lượt kết thúc bằng game over.
- bestSurvivalMs là thời gian cao nhất của lượt đã hoàn tất.
- Khi thua, đóng băng kết quả, ghi dữ liệu rồi hiển thị thưởng đã lưu.
- Trong khi ghi thưởng, vô hiệu hóa Chơi lại và Trang chủ để tránh giao dịch chồng.
- Nếu ghi thất bại, giữ kết quả trong bộ nhớ, hiển thị Thử lưu lại; không thông báo đã nhận tiền khi chưa lưu thành công.
- Bỏ lượt không cộng tiền, không tăng totalRuns và không cập nhật kỷ lục.

### 9.1 Nâng cấp

| Nhánh | Cấp mua thêm | Hiệu quả mỗi cấp | Giá lần lượt |
|---|---:|---|---|
| Giáp | 2 | +1 giáp tối đa | 200 / 500 |
| Khiên | 3 | +1 giây thời hạn | 100 / 250 / 500 |
| Làm chậm | 3 | +0,5 giây thời hạn | 100 / 250 / 500 |

```text
maxArmor = 3 + armorUpgradeLevel
shieldDurationSeconds = 8 + shieldUpgradeLevel
slowDurationSeconds = 5 + slowUpgradeLevel × 0.5
```

Chụp cấu hình nâng cấp khi bắt đầu lượt. Không thay đổi thuộc tính giữa lượt. Không nâng cấp bán kính, sức sát thương hoặc hệ số làm chậm trong MVP. Không hoàn tiền hoặc bán nâng cấp.

Mỗi lần mua phải kiểm tra số dư và cấp tối đa từ dữ liệu mới nhất, sau đó trừ tiền và tăng cấp trong cùng giao dịch. Khóa nút khi đang mua. Nếu lưu lỗi, không hiển thị mua thành công.

## 10. Đặc tả màn hình

### 10.1 Trang chủ

Tên game; hình tàu; kỷ lục; tổng tiền; nút Chơi nổi bật; các nút Nâng cấp, Cài đặt, Hướng dẫn. Trong thời gian tải tiến trình, hiển thị trạng thái tải và khóa Chơi/Mua. Không hiển thị dữ liệu mặc định như thể đã tải xong.

### 10.2 Hướng dẫn lần đầu

Hiển thị dòng Kéo ngang để né thiên thạch. Chưa chạy mô phỏng. Lần kéo đầu tiên hoàn thành hướng dẫn, lưu tutorialCompleted và bắt đầu RUNNING; mốc kéo được tạo ngay từ thao tác đó. Vật cản đầu tiên vẫn chỉ xuất hiện sau hai giây chạy. Màn hướng dẫn mở từ menu chỉ giải thích và quay về, không xóa cờ đã học.

### 10.3 Gameplay

- Trên trái: giáp dạng biểu tượng và giá trị hiện tại/tối đa.
- Trên giữa: điểm.
- Trên phải: nút tạm dừng.
- Dưới HUD: biểu tượng hiệu ứng và số giây còn lại, làm tròn lên.
- Giữa màn hình: vùng rơi thiên thạch.
- Phía dưới: tàu.
- Nền sao tối có độ tương phản thấp; vật cản và vật phẩm phải dễ phân biệt.

Các lượt sau hướng dẫn dùng đếm ngược 3, 2, 1, mỗi số một giây. Không cho điều khiển tàu hoặc chạy đồng hồ trong COUNTDOWN.

### 10.4 Tạm dừng

Lớp phủ tối với Tiếp tục, Chơi lại, Về trang chủ. Tiếp tục chạy đếm ngược ba giây rồi quay lại RUNNING, giữ nguyên thế giới và hiệu ứng. Chơi lại/Về trang chủ yêu cầu xác nhận: Bỏ lượt hiện tại? Bạn sẽ không nhận thưởng của lượt này. Hai lựa chọn Tiếp tục chơi và Bỏ lượt.

### 10.5 Kết quả

Điểm cuối, kỷ lục, thời gian sống ở dạng mm:ss, tiền vừa nhận, tổng tiền, nhãn Kỷ lục mới nếu phù hợp. Nút Chơi lại và Trang chủ. Không tự bắt đầu lượt mới. Không có nút xem quảng cáo hoặc hồi sinh.

### 10.6 Nâng cấp

Hiển thị số dư và ba mục nâng cấp. Mỗi mục có cấp hiện tại, hiệu quả hiện tại, hiệu quả cấp tiếp theo và giá. Trạng thái nút: Mua, Không đủ tiền, Đã tối đa, Đang lưu. Mua xong cập nhật số dư và cấp ngay từ dữ liệu đã lưu.

### 10.7 Cài đặt

Bật/tắt nhạc, hiệu ứng âm thanh, rung, giảm chuyển động. Mặc định ba mục đầu bật, giảm chuyển động tắt. Có Xóa tiến trình với xác nhận rõ hậu quả. Xóa tiến trình đặt tiền, thành tích, nâng cấp và hướng dẫn về mặc định; giữ tùy chọn âm thanh/rung/chuyển động. Sau thành công về trang chủ.

## 11. Hình ảnh, âm thanh và tiếp cận

Phong cách không gian 2D đơn giản: nền xanh đen, tàu xanh lam sáng, thiên thạch xám/nâu, khiên xanh cyan, sửa chữa hình dấu cộng, làm chậm hình đồng hồ. Phân biệt vật phẩm bằng hình và biểu tượng, không chỉ màu.

Tài nguyên cần có: tàu, ba thiên thạch, ba vật phẩm, giáp, tiền, pause, hiệu ứng trúng đòn, khiên, hạt nền; một nhạc nền lặp và âm thanh nhặt đồ, trúng đòn, vỡ khiên, kết thúc, mua nâng cấp. Có thể dùng hình học/vector tạm khi phát triển. Mọi asset cuối cùng phải có nguồn và quyền sử dụng rõ ràng.

- Hiệu ứng trúng đòn ngắn, không lóe sáng toàn màn hình liên tục.
- Giảm chuyển động tắt rung màn hình, giảm hạt và dừng cuộn nền.
- Tắt rung vô hiệu hóa mọi rung thiết bị.
- Vùng bấm menu tối thiểu 48dp; có nhãn tiếp cận cho icon.
- Menu hỗ trợ font hệ thống lớn; tránh chữ bị cắt.
- Gameplay thời gian thực cần thao tác nhìn/kéo; không tuyên bố hỗ trợ chơi hoàn toàn bằng trình đọc màn hình.
- Mất foreground hoặc mất audio focus: dừng âm thanh và pause lượt đang chạy. Không tự phát lại nhạc gameplay trước khi người chơi tiếp tục.
- Âm thanh và rung chỉ phát theo sự kiện một lần, không phát lại do recomposition.

## 12. Kiến trúc triển khai

Kotlin; Jetpack Compose cho menu/HUD; Canvas cho thế giới 2D; Preferences DataStore cho tiến trình và cài đặt. Chọn phiên bản SDK, Gradle và thư viện ổn định tương thích tại thời điểm triển khai; ghi phiên bản đã dùng trong README. Đề xuất minSdk 26, cần xác nhận theo nhóm thiết bị thực tế; targetSdk theo yêu cầu phát hành tại thời điểm đó.

```text
app/
  core/config/
  core/audio/
  core/storage/
  game/engine/
  game/entities/
  game/collision/
  game/spawning/
  game/scoring/
  feature/home/
  feature/gameplay/
  feature/result/
  feature/upgrades/
  feature/settings/
  ui/theme/
```

| Thành phần | Trách nhiệm |
|---|---|
| GameConfig | Toàn bộ thông số gameplay, giá và giới hạn |
| GameEngine | Bước mô phỏng cố định, không phụ thuộc Android |
| GameState | Trạng thái tàu, đối tượng, hiệu ứng, đồng hồ |
| GameRenderer | Vẽ snapshot thế giới, không sửa gameplay |
| SpawnSystem | Lịch sinh, random có seed, kiểm tra khoảng trống |
| CollisionSystem | Va chạm, thứ tự xử lý, bất tử |
| ScoreCalculator | Tính điểm và thưởng |
| ProgressRepository | Tiến trình và giao dịch thưởng/mua |
| SettingsRepository | Tùy chọn người dùng |
| AudioController | Audio focus, nhạc và hiệu ứng |
| GameplayViewModel | Kết nối engine, UI, lifecycle và lưu kết quả |

Engine có một chủ thể sở hữu và cập nhật; không mutate thế giới từ nhiều luồng. Render đọc snapshot hoặc dữ liệu trên cùng luồng theo thiết kế nhất quán. Không khởi tạo vòng lặp từ mỗi lần recomposition. Hủy vòng lặp và giải phóng âm thanh đúng lifecycle. Không phát toàn bộ trạng thái game qua Compose mỗi khung hình; cập nhật HUD khi giá trị hiển thị đổi. Không đọc/ghi storage trong render loop.

### 12.1 Máy trạng thái

```text
READY → COUNTDOWN → RUNNING → GAME_OVER
READY → RUNNING (hướng dẫn lần đầu)
RUNNING → PAUSED → COUNTDOWN → RUNNING
COUNTDOWN → PAUSED
PAUSED → READY (bỏ lượt hoặc chơi lại)
```

Chỉ RUNNING cập nhật mô phỏng. GAME_OVER chỉ phát sinh một lần cho mỗi runId. Nếu app ra nền trong COUNTDOWN thì chuyển PAUSED; tiếp tục sẽ đếm lại từ 3. Điều hướng rời gameplay phải dừng engine trước khi tạo lượt khác.

## 13. Mô hình dữ liệu và tính nhất quán

```text
PlayerProgress
  schemaVersion: Int = 1
  credits: Long = 0
  bestScore: Long = 0
  totalRuns: Long = 0
  bestSurvivalMs: Long = 0
  armorUpgradeLevel: Int = 0
  shieldUpgradeLevel: Int = 0
  slowUpgradeLevel: Int = 0
  lastRewardedRunId: String? = null

UserSettings
  musicEnabled: Boolean = true
  soundEnabled: Boolean = true
  vibrationEnabled: Boolean = true
  reducedMotionEnabled: Boolean = false
  tutorialCompleted: Boolean = false

RunResult
  runId: String
  score: Long
  survivalMs: Long
  collectedItems: Int
  earnedCredits: Long

RuntimeGameState
  phase
  shipPosition / previousShipPosition
  armor / maxArmor
  invulnerabilityRemaining
  shieldRemaining / slowRemaining
  survivalTime
  nextMeteorSpawnTime / nextItemSpawnTime
  meteors / items / particles
  collectedItems
  runId / randomSeed
```

RuntimeGameState không lưu xuống disk trong MVP. Mỗi lượt dùng UUID mới. Không hỗ trợ tiếp tục lượt sau process death; lần mở sau về trang chủ. Tiến trình đã lưu vẫn tồn tại.

Ghi thưởng bằng một giao dịch: kiểm tra runId chưa được thưởng, cộng tiền, cập nhật kỷ lục/số lượt/thời gian, ghi lastRewardedRunId. Một hàng đợi tuần tự xử lý ghi thưởng và không cho bắt đầu lượt mới trước khi hoàn tất, vì lastRewardedRunId chỉ chống lặp cho lượt gần nhất. Nếu sau này cho phép nhiều kết quả chờ ghi, phải chuyển sang danh sách/ledger runId đã xử lý.

Giá trị đọc từ disk phải được kiểm tra: tiền/điểm không âm, cấp trong giới hạn. Thiếu key dùng mặc định. Có migration theo schemaVersion; không âm thầm xóa dữ liệu phiên bản mới hơn chưa hiểu. Khi dữ liệu hỏng không phục hồi được, dùng corruption handler và thông báo tiến trình đã được đặt lại. Không để lỗi đọc làm app crash.

MVP không có backend nên không bảo đảm chống sửa dữ liệu trên thiết bị. Không thu thập dữ liệu cá nhân, không cần quyền Internet, vị trí, danh bạ hoặc bộ nhớ dùng chung. Quyền rung chỉ khai báo khi có triển khai rung. Quyết định backup Android phải được ghi trong README; không xem backup hệ điều hành là cloud save của game.

## 14. Lifecycle và tình huống biên

| Tình huống | Kết quả bắt buộc |
|---|---|
| Bấm Home, khóa màn hình, mất foreground | Tự pause, dừng âm thanh |
| Trở lại app | Hiện pause, không tự chạy |
| Back khi đang chơi | Pause |
| Back khi đang pause | Tiếp tục theo luồng đếm ngược |
| Back tại trang chủ | Hành vi thoát chuẩn Android |
| Process death giữa lượt | Về trang chủ khi mở lại; không thưởng lượt dang dở |
| Thiết bị đổi giờ hệ thống | Không ảnh hưởng đồng hồ gameplay |
| Nhiều vật cản chạm cùng bước | Tối đa một lần mất giáp |
| Khiên và sửa chữa được nhặt cùng bước | Cả hai áp dụng, mỗi vật phẩm tính một lần |
| Hiệu ứng hết hạn đúng bước va chạm | Hết hạn trước khi xử lý va chạm |
| Nhấn Chơi/Mua nhiều lần | Một thao tác hợp lệ được xử lý tại một thời điểm |
| Lưu thất bại | Có trạng thái lỗi và thử lại, không báo thành công giả |
| Không có kết nối | Chơi và lưu bình thường |
| UI được tạo lại | Không tạo engine trùng hoặc nhận thưởng lặp |

## 15. Hiệu năng và giới hạn

- Mục tiêu 60 FPS trên thiết bị tầm trung được chỉ định trước khi nghiệm thu.
- Tối đa 80 thiên thạch, hai vật phẩm, 150 hạt.
- Cache hình/brush/tài nguyên phù hợp; tránh tạo đối tượng không cần thiết mỗi frame.
- Không có disk I/O hoặc tải asset nặng trên đường render.
- Không tăng dần bộ nhớ sau nhiều lượt chơi; kiểm tra bằng profiler.
- Chơi được hoàn toàn trong chế độ máy bay.
- Tối thiểu kiểm tra ba tỷ lệ màn hình và font hệ thống lớn.
- FPS, thời gian khởi động và mức dùng bộ nhớ phải được đo trên thiết bị ghi rõ; không tuyên bố đạt chỉ dựa vào emulator.

## 16. Kiểm thử và nghiệm thu

### 16.1 Unit test có ý nghĩa

- Điểm tại 0 giây, giây lẻ và có vật phẩm; thưởng làm tròn xuống.
- Độ khó tại 19,99/20/59,99/60 giây và tại các giới hạn hệ số.
- Va chạm đúng biên, không chạm, di chuyển nhanh qua vật cản.
- Bất tử, khiên bị tiêu thụ, hiệu ứng hết hạn, nhiều va chạm cùng bước.
- Sửa chữa clamp giáp; vật phẩm lặp không cộng chồng sai.
- Spawn đúng seed, giới hạn số lượng, tám lần thử và quy tắc khoảng trống.
- Pause không tăng thời gian, điểm, vị trí và thời hạn hiệu ứng.
- Mua thiếu tiền, đạt tối đa, bấm mua lặp và lưu thất bại.
- Ghi thưởng cùng runId nhiều lần chỉ cộng một lần.
- Dữ liệu thiếu key, cấp ngoài giới hạn và migration được hỗ trợ.

### 16.2 Kiểm thử tích hợp/thủ công

1. Cài mới → hướng dẫn → chơi → thua → nhận thưởng → chơi lại.
2. Mua nâng cấp → đóng và mở app → kiểm tra tiền/cấp → bắt đầu lượt mới.
3. Ra nền khi đang chơi và khi đang đếm ngược → quay lại → tiếp tục.
4. Hủy tiến trình giữa lượt và sau khi ghi thưởng → kiểm tra dữ liệu.
5. Tắt từng tùy chọn âm thanh/rung → kiểm tra áp dụng ngay và lưu.
6. Kéo sát cạnh, chạm đa điểm, chạm pause trong lúc kéo.
7. Chơi ít nhất 20 lượt liên tục → kiểm tra bộ nhớ, âm thanh và engine trùng.
8. Chạy chế độ máy bay, font lớn và nhiều tỷ lệ màn hình.
9. Thử lỗi lưu có kiểm soát → thử lại → không cộng tiền hai lần.
10. Xóa tiến trình → xác nhận dữ liệu gameplay reset và tùy chọn được giữ.

### 16.3 Definition of Done

- Build debug thành công trong môi trường hỗ trợ.
- Các kiểm thử liên quan vượt qua; báo rõ phần chưa chạy được.
- Luồng hoàn chỉnh không còn TODO hoặc placeholder chức năng.
- Không crash trong checklist thủ công đã thực hiện.
- Thông số cân bằng tập trung trong GameConfig.
- README có hướng dẫn build, chạy, kiến trúc và phiên bản công cụ.
- Có danh mục asset và giấy phép.
- APK debug được tạo để review; ký release và phát hành là công việc riêng.

## 17. Kế hoạch chia gói cho AI

| Gói | Phạm vi | Phụ thuộc | Nghiệm thu |
|---|---|---|---|
| 1 | Dự án Kotlin, Compose, theme, điều hướng | Không | Build được, mở các màn hình khung |
| 2 | Model, GameConfig, engine và máy trạng thái | 1 | Mô phỏng độc lập Android, test chuyển trạng thái |
| 3 | Canvas, scale, tàu và chạm | 2 | Kéo đúng tọa độ, không nhảy tàu |
| 4 | Thiên thạch, spawn và độ khó | 3 | Đúng lịch sinh, giới hạn và pause |
| 5 | Va chạm, giáp, game over | 4 | Không mất nhiều giáp cùng bước, kết thúc một lần |
| 6 | Vật phẩm và thời hạn hiệu ứng | 5 | Đúng ưu tiên xử lý và nhặt lặp |
| 7 | Điểm, kết quả, repository và thưởng | 6 | Lưu bền vững, thưởng không lặp |
| 8 | Nâng cấp và giao dịch mua | 7 | Trừ tiền/tăng cấp nguyên tử, áp dụng lượt sau |
| 9 | Cài đặt, âm thanh và lifecycle | 8 | Pause đúng, tùy chọn lưu và áp dụng |
| 10 | Asset, hiệu ứng, tiếp cận và QA | 9 | Checklist, README và APK debug |

Mỗi gói phải giữ các gói trước hoạt động. Làm bản chạy được sau gói 3 để kiểm tra cảm giác điều khiển, nhưng không coi đó là MVP đã hoàn tất.

## 18. Prompt triển khai dùng lại

```text
Bạn là lập trình viên Android native. Triển khai game Né Thiên Thạch theo
file đặc tả đính kèm. Đọc mã nguồn, README và hướng dẫn repository trước.

Trong lượt này chỉ triển khai GÓI [SỐ]: [TÊN].

Yêu cầu:
1. Hoàn thành toàn bộ phạm vi và tiêu chí nghiệm thu của gói.
2. Dùng Kotlin, Compose cho UI và Canvas cho gameplay.
3. Logic engine độc lập Android; mọi thông số nằm trong GameConfig.
4. Giữ các tính năng đã có hoạt động, không tự thêm phạm vi ngoài MVP.
5. Không để TODO trong luồng đã nhận triển khai.
6. Thêm kiểm thử có ý nghĩa cho logic được thay đổi.
7. Chạy build và kiểm thử phù hợp nếu môi trường hỗ trợ.
8. Nếu có mâu thuẫn, nêu rõ và chọn giải pháp ít ảnh hưởng nhất;
   ghi lại giả định làm thay đổi hành vi sản phẩm.
9. Không tuyên bố đã test trên thiết bị hoặc build thành công nếu chưa chạy.

Báo cáo cuối:
- Chức năng hoàn thành và file chính thay đổi.
- Kết quả build/kiểm thử thực tế.
- Giới hạn và phần chưa kiểm chứng.
- Gói tiếp theo có thể triển khai.
```

## 19. Những quyết định cần xác nhận trước phát hành

- Tên chính thức, biểu tượng và bộ asset cuối cùng.
- Thiết bị chuẩn để đo hiệu năng và danh sách Android hỗ trợ.
- Độ khó, giá nâng cấp và thời lượng lượt chơi sau playtest.
- Package/applicationId và thông tin nhà phát hành.
- Cấu hình backup, chữ ký release và kênh phân phối.

Các quyết định này không ngăn việc bắt đầu triển khai MVP theo tài liệu.
