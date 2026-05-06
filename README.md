# Unit Test FJob

Dự án này chứa các script unit test và báo cáo kết quả kiểm thử cho các dịch vụ của hệ thống FJob.

## Cấu trúc thư mục

- `Admin/`: Unit test cho Admin Service.
- `Auth & User Common/`: Unit test cho Authentication và User Common Service.
- `Candidate Management/`: Unit test cho quản lý ứng viên và freelancer.
- `Job/`: Unit test cho Job Service.
- `Notification & Schedule/`: Unit test cho Email và lập lịch (Schedule).
- `Recruiter Management/`: Unit test cho quản lý nhà tuyển dụng.
- `Search Service/`: Unit test cho dịch vụ tìm kiếm.
- `User Utilities/`: Unit test cho các tiện ích người dùng (Standout, Settings).

## Báo cáo Độ phủ (Test Coverage)

Chi tiết độ phủ của các script test đối với source code gốc được tổng hợp tại file:
[unit-test-coverage.csv](./unit-test-coverage.csv)

### Tóm tắt độ phủ:

| Chỉ số | Giá trị |
| :--- | :--- |
| **Tổng số Method** | 153 |
| **Số Method đã test** | 136 |
| **Tỷ lệ độ phủ** | **88.9%** |
| **Tổng số Test Case** | 412 |
| **Số Test Case Pass** | 333 |
| **Số Test Case Fail** | 77 |

## Hướng dẫn chạy test

Các test case được viết bằng JUnit 5 và Mockito. Để chạy test, bạn có thể sử dụng Maven hoặc chạy trực tiếp trong IDE (IntelliJ IDEA).

```bash
mvn test
```

## Ghi chú

Các file `.csv` trong mỗi thư mục chứa chi tiết từng test case, bao gồm kịch bản, dữ liệu đầu vào, kết quả mong đợi và trạng thái thực thi thực tế.
