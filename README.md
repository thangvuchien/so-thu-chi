# Sổ Thu Chi – Android

Ứng dụng Android offline quản lý thu nhập, chi tiêu và mục tiêu tiết kiệm. Thiết kế tối giản, không quảng cáo, không đăng nhập, không chạy nền.

## Tính năng
- Trang hôm nay: thu, chi, chênh lệch và giao dịch trong ngày.
- Thêm giao dịch thu/chi, chọn ngày, danh mục và ghi chú.
- Lịch sử giao dịch theo ngày; xóa giao dịch.
- Thống kê tháng, nhóm chi.
- Mục tiêu tiết kiệm chỉnh được (mặc định 10.000.000 VND).
- Lưu cục bộ SQLite.

## Build APK
Yêu cầu Android Studio (Giraffe trở lên), JDK 17, Android SDK 35.
1. Mở thư mục project bằng Android Studio.
2. Chờ Gradle Sync.
3. Build > Build APK(s).
4. APK debug nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

Lưu ý: môi trường tạo file hiện tại không có Android SDK/Gradle nên chưa thể biên dịch và kiểm thử APK trong phiên này. Source project được đóng gói sẵn.
