# Bắt đầu chuyển dự án sang Gemini

Bộ bàn giao được cập nhật sau Phase 4 ngày 29/09/2026. Prompt mới để dán trực tiếp vào Gemini 3.8 Flash: [PROMPT-GEMINI-TIEP-TUC.md](PROMPT-GEMINI-TIEP-TUC.md). Trạng thái thực tế nằm trong [CURRENT_TASK.md](CURRENT_TASK.md). Không cần chép lại toàn bộ cuộc trò chuyện.

## Bạn làm gì tiếp theo?

1. Mở đúng thư mục **D:/Motorbike-rental** trong Antigravity. Chỉ để một agent sửa dự án tại một thời điểm; dừng lượt sửa ở Codex trước khi yêu cầu Gemini sửa.
2. Chọn model Gemini mà tài khoản của bạn có quyền dùng; bảo đảm agent có công cụ đọc/sửa file, terminal và trình duyệt. Không phải cửa sổ chat nào cũng có các công cụ này.
3. Mở [PROMPTS.md](PROMPTS.md), gửi **prompt 1 — Read-only takeover**. Gemini phải đối chiếu tài liệu với code trước; không sửa code/database ở bước này.
4. Đọc bản tóm tắt và kiểm tra Gemini nêu đúng phase đang dở, kết quả test thật, việc chưa làm. Sau đó gửi **prompt 2 — Continue the authorized phase** để nó code, chạy test và sửa lỗi trong phạm vi đã duyệt.
5. Khi chuyển cuộc trò chuyện hoặc gần hết hạn mức, dùng **prompt 4 — Save context**. Không đợi đến token cuối cùng mới ghi tiến độ.

## Tài liệu nào dùng làm gì?

- [AGENTS.md](../../AGENTS.md): quy tắc tiếng Anh đã điều chỉnh theo dự án, giữ các nguyên tắc bạn đưa ra.
- [GEMINI.md](../../GEMINI.md): điểm dẫn tới quy tắc chung cho Gemini; không sao chép nhiều bản nghiệp vụ dễ mâu thuẫn.
- [PROJECT_CONTEXT.md](../../PROJECT_CONTEXT.md): stack, quan hệ dữ liệu, phân quyền, API, điều đã xác minh và thiếu sót.
- [CURRENT_TASK.md](CURRENT_TASK.md): **đọc đầu tiên để biết tiếp tục ở đâu**, tiến trình/database/bản chạy/test và việc cần làm tiếp.
- [BUSINESS_RULES.md](BUSINESS_RULES.md): nghiệp vụ đầy đủ theo 9 phase, cả các yêu cầu cũ cần giữ và quyết định đã bị yêu cầu mới thay thế.
- [USER_REQUIREMENTS_ORIGINAL.txt](USER_REQUIREMENTS_ORIGINAL.txt): nguyên văn bản yêu cầu tiếng Việt và 65 tình huống nghiệm thu, được sao chép từ file bạn đã cung cấp.
- [VERIFICATION.md](VERIFICATION.md): lệnh build/test/MySQL/khởi động, cách dọn dữ liệu thử và phục hồi an toàn.
- [PROMPTS.md](PROMPTS.md): bốn prompt tiếng Anh dùng ngay.
- [ACCEPTANCE_CASES.md](ACCEPTANCE_CASES.md): 65 tình huống, cần ghi bằng chứng từng mục thay vì suy từ số test.
- [SOURCE_INDEX.md](SOURCE_INDEX.md): danh sách file nguồn để Gemini đọc theo module.
- [APPROVED_PLAN_REFERENCE.txt](APPROVED_PLAN_REFERENCE.txt): bản đọc được bằng text của kế hoạch đã duyệt; trạng thái trong kế hoạch là lịch sử, phải đối chiếu checkpoint mới.

## Bộ prompt của bạn có giúp Gemini làm như vậy được không?

Có thể dùng Gemini 3.8 Flash cho quy trình lập trình có công cụ: Google mô tả model hỗ trợ tác vụ kỹ thuật phần mềm dài và function calling/code execution. Đây là khả năng của model, không bảo đảm tự động sửa đúng dự án này hay đạt kết quả tương đương một agent khác. Chưa có kiểm thử so sánh hai model trên dự án của bạn. [Tài liệu model của Google](https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash).

Antigravity hiện tài liệu hóa việc đọc AGENTS.md/GEMINI.md theo thư mục. Tôi đã tạo cả quy tắc chung và file dẫn hướng; prompt tiếp quản vẫn yêu cầu đọc rõ từng file để không phụ thuộc hoàn toàn vào tự nhận diện của phiên bản ứng dụng. [Quy tắc Antigravity](https://antigravity.google/docs/rules/).

Những bổ sung quan trọng so với prompt chung: biết chính xác code đang dở ở đâu; không nhầm yêu cầu với triển khai; lệnh thực thi đúng máy Windows này; test tranh chấp MySQL; lưu bằng chứng; tránh sửa đồng thời; kế thừa phê duyệt đã có; hỏi đúng hai quy tắc giá/khấu trừ chưa chốt. Prompt không tự cấp quyền terminal, trình duyệt hoặc database, và không thay thế việc chạy test.

Không cần thiết lập MySQL MCP nếu Gemini chạy được Java/JDBC hiện có. Không gửi nội dung application.properties, backup SQL, access token hoặc ảnh CCCD vào chat/cloud để 'nạp ngữ cảnh'. Những file bàn giao này không chứa mật khẩu hay khóa API.

## Cách đánh giá kết quả Gemini

Yêu cầu đường dẫn log, câu lệnh đã chạy và exit code; chỉ 'đã kiểm tra' hoặc 'build ok' là chưa đủ. Với luồng tiền/lịch phải có kiểm thử trùng/lặp/sai/trễ và kiểm tra trên MySQL clone. Với UI cần thao tác thực tế, không chỉ HTTP 200. Chỉ kết luận xong toàn dự án khi nghiệm thu đủ các phase và 65 tình huống, không dựa trên số lượng test hiện có.
