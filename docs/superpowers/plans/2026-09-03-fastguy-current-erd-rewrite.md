# FastGuy Current ERD Rewrite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Viết lại `docs/erd.md` để phản ánh đầy đủ schema FastGuy hiện tại và bổ sung ERD Level 1 theo nhãn quan hệ bằng động từ.

**Architecture:** Dùng schema SQL và migration đến 065 làm nguồn cột/ràng buộc, JPA để đối chiếu mapping. Tài liệu gồm danh mục bảng, Mermaid theo nhóm, bảng quan hệ và data dictionary đầy đủ; sai lệch giữa hai baseline được công khai.

**Tech Stack:** Markdown, Mermaid flowchart, Microsoft SQL Server DDL.

## Global Constraints

- Chỉ sửa `docs/erd.md`, ngoài spec/plan đã được người dùng duyệt.
- Không kết nối hoặc thay đổi database.
- Bao phủ đủ 39 bảng và toàn bộ cột/ràng buộc/index trong schema hiện hành.
- ERD Level 1 dùng `Entity -- "động từ" --> Entity`, không ghi cardinality trong sơ đồ.
- Không giữ bảng đã bị migration 051 xóa trong schema hiện hành.
- Không commit.

---

### Task 1: Viết lại tổng quan và ERD Level 1

**Files:**
- Modify: `docs/erd.md`

- [ ] Giữ phần tổng quan, nguồn chuẩn, mốc migration 065 và danh sách đủ 39 bảng.
- [ ] Thay toàn bộ sáu khối Mermaid bằng đúng một `flowchart` có đủ 39 node, không có thuộc tính.
- [ ] Dùng `subgraph` để nhóm các node; bảng không có FK vẫn xuất hiện độc lập và không được nối bằng quan hệ giả.
- [ ] Đổi mọi nhãn cạnh sang động từ tiếng Anh tự nhiên như `has`, `places`, `contains`, `belongs to`, `records`, `uses`, `creates`, `approves`, `settles`.
- [ ] Giữ bảng quan hệ riêng để diễn giải cardinality và FK.
- [ ] Kiểm tra Mermaid có đúng 39 node, đúng một block, và không dùng ký hiệu `1 -- N` hoặc thuộc tính.

### Task 2: Viết data dictionary đầy đủ

**Files:**
- Modify: `docs/erd.md`

- [ ] Trích đủ cột của 39 bảng theo DDL hiện hành.
- [ ] Ghi kiểu, nullability, default, PK, FK, UNIQUE và CHECK.
- [ ] Ghi index và trigger quan trọng cho từng bảng.
- [ ] Đánh dấu bảng migration/archive/configuration và mapping JPA đặc biệt.

### Task 3: Kiểm tra độ chính xác và phạm vi

**Files:**
- Verify: `docs/erd.md`

- [ ] So sánh danh sách tiêu đề bảng trong tài liệu với danh sách `CREATE TABLE` của `database/init.sql`.
- [ ] Xác nhận đủ 39 bảng, không thiếu, không thừa.
- [ ] Xác nhận năm bảng đã xóa chỉ xuất hiện trong ghi chú lịch sử, không trong data dictionary hiện hành.
- [ ] Kiểm tra Mermaid fences cân bằng, không có placeholder và `git diff --check` thành công.
- [ ] Kiểm tra Git chỉ sửa/tạo ba file thuộc công việc ERD; không stage hoặc commit.
