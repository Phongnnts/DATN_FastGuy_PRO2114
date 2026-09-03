# FastGuy Business Defense Question Bank Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tạo một file Markdown gồm đúng 100 câu hỏi và câu trả lời dễ hiểu để ôn thi bảo vệ nghiệp vụ FastGuy.

**Architecture:** Nội dung được tổng hợp từ tài liệu nghiệp vụ hiện có, chia thành 10 nhóm theo hành trình vận hành từ khách hàng đến quản trị. Mỗi câu trả lời giải thích tác nhân, điều kiện, xử lý và lý do nghiệp vụ; không thay đổi code hoặc tài liệu hiện có.

**Tech Stack:** Markdown, tài liệu dự án FastGuy.

## Global Constraints

- Chỉ tạo `docs/100-cau-hoi-van-dap-nghiep-vu-fastguy.md`.
- Đúng 100 câu, đánh số liên tục, chia 10 nhóm × 10 câu.
- Mỗi câu trả lời khoảng 3–6 câu, dễ hiểu và không quá kỹ thuật.
- Không mô tả nội dung chỉ có trong thiết kế/kế hoạch như chức năng chắc chắn đã triển khai.
- Không sửa hoặc ghi đè tài liệu hiện có.
- Không commit nếu người dùng chưa yêu cầu.

---

### Task 1: Soạn 50 câu về nền tảng và hành trình đơn hàng

**Files:**
- Create: `docs/100-cau-hoi-van-dap-nghiep-vu-fastguy.md`

**Interfaces:**
- Consumes: `docs/usecase.md`, `luong-nghiep-vu-fastguy.md`, các đặc tả nghiệp vụ liên quan trong `docs/superpowers/specs/`.
- Produces: Câu 1–50 thuộc năm nhóm đầu của tài liệu.

- [ ] **Step 1: Tạo tiêu đề, hướng dẫn sử dụng và mục lục 10 nhóm**

Nêu rõ tài liệu thiên về nghiệp vụ, cách trả lời ngắn gọn trước hội đồng và cách dùng ví dụ thực tế.

- [ ] **Step 2: Viết câu 1–10 — tổng quan và phạm vi**

Bao phủ mục tiêu, giá trị, phạm vi một cửa hàng, điểm khác website bán hàng đơn giản và giới hạn hệ thống.

- [ ] **Step 3: Viết câu 11–20 — vai trò và phân quyền**

Bao phủ Guest, User, Staff, Shipper, Admin; ownership, trạng thái tài khoản và giới hạn trách nhiệm.

- [ ] **Step 4: Viết câu 21–30 — menu, giỏ hàng và checkout**

Bao phủ lựa chọn món, biến thể, món thêm, giỏ guest/user, tính lại giá, địa chỉ, phí giao và chống tạo đơn trùng.

- [ ] **Step 5: Viết câu 31–40 — thanh toán, hủy và hoàn tiền**

Bao phủ COD, chuyển khoản, xác nhận thanh toán, timeout, hủy đơn, refund pending và tránh hoàn tiền lặp.

- [ ] **Step 6: Viết câu 41–50 — tiếp nhận và chế biến**

Bao phủ các trạng thái từ chờ xác nhận đến sẵn sàng giao, điều kiện thao tác của Staff, xử lý đơn trễ và giữ lịch sử.

- [ ] **Step 7: Kiểm tra phần 1**

Run:

```powershell
$path = 'docs/100-cau-hoi-van-dap-nghiep-vu-fastguy.md'; $text = Get-Content -Raw -LiteralPath $path; ([regex]::Matches($text, '(?m)^### Câu ([1-9]|[1-4][0-9]|50):')).Count
```

Expected: `50`.

### Task 2: Hoàn thiện 50 câu về vận hành và quản trị

**Files:**
- Modify: `docs/100-cau-hoi-van-dap-nghiep-vu-fastguy.md`

**Interfaces:**
- Consumes: Câu 1–50 từ Task 1 và tài liệu nghiệp vụ kho, giao hàng, hậu mãi, quản trị.
- Produces: Câu 51–100 và tài liệu hoàn chỉnh.

- [ ] **Step 1: Viết câu 51–60 — phân công và giao hàng**

Bao phủ điều kiện gán shipper, nhận hàng, COD, giao thất bại, giao lại, đổi shipper và trả về cửa hàng.

- [ ] **Step 2: Viết câu 61–70 — kho nguyên liệu**

Bao phủ nguyên liệu, công thức, nhập hàng, giữ/tiêu hao tồn, kiểm kê, hao hụt, tồn khả dụng và giá vốn bình quân.

- [ ] **Step 3: Viết câu 71–80 — khuyến mãi và khách hàng thân thiết**

Bao phủ coupon, điều kiện áp dụng, lượt dùng, ví coupon, tích điểm, hoàn tác điểm và banner/chương trình tiếp thị.

- [ ] **Step 4: Viết câu 81–90 — hậu mãi**

Bao phủ đánh giá, yêu cầu hỗ trợ, thông báo, tra cứu đơn guest, bảo vệ thông tin và xử lý phản ánh.

- [ ] **Step 5: Viết câu 91–100 — quản trị, báo cáo và ngoại lệ**

Bao phủ dashboard, báo cáo vận hành, dữ liệu lịch sử, validation, xử lý đồng thời, lỗi tích hợp, tính nhất quán và giới hạn dự án.

- [ ] **Step 6: Kiểm tra số lượng, thứ tự và cấu trúc**

Run:

```powershell
$path = 'docs/100-cau-hoi-van-dap-nghiep-vu-fastguy.md'; $text = Get-Content -Raw -LiteralPath $path; $numbers = [regex]::Matches($text, '(?m)^### Câu (\d+):') | ForEach-Object { [int]$_.Groups[1].Value }; [pscustomobject]@{ Questions=$numbers.Count; Sequential=(($numbers -join ',') -eq ((1..100) -join ',')); Answers=([regex]::Matches($text, '(?m)^\*\*Trả lời:\*\*')).Count }
```

Expected: `Questions = 100`, `Sequential = True`, `Answers = 100`.

- [ ] **Step 7: Kiểm tra nội dung dễ hiểu và không có placeholder**

Run:

```powershell
rg -n "TBD|TODO|implement later|đang cập nhật|chưa viết" "docs/100-cau-hoi-van-dap-nghiep-vu-fastguy.md"
```

Expected: không có kết quả.

- [ ] **Step 8: Kiểm tra Git chỉ có file dự kiến**

Run:

```powershell
git status --short -- "docs/100-cau-hoi-van-dap-nghiep-vu-fastguy.md" "docs/superpowers/specs/2026-09-03-fastguy-business-defense-question-bank-design.md" "docs/superpowers/plans/2026-09-03-fastguy-business-defense-question-bank.md"
```

Expected: chỉ ba tài liệu của công việc này xuất hiện; không stage hoặc commit.
