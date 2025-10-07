1. Normalize văn bản + matching nhất quán
   - Vì sao: Tránh miss khi user gõ “hài/hài hước/rom-com/Comedy”, tên có dấu/không dấu.
   - Làm gì: Hàm normalize() (lowercase, bỏ dấu, trim, collapse spaces) áp cho: query genres, movie genres, movie titles, name patterns.
   - Tiêu chí xong: Tìm “Hài” khớp được với slug comedy, tên có dấu/không dấu đều bắt được.
2. Age gate dựa trên “minAge của cả nhóm”
   - Vì sao: Đảm bảo mọi gợi ý an toàn tuổi theo người nhỏ nhất trong nhóm.
   - Làm gì: Map MovieAge → minRequiredAge, lấy groupMinAge = min(allAges), lọc movie.minAge <= groupMinAge.
   - Tiêu chí xong: Không còn phim vượt tuổi lọt vào list.
3. Chuyển từ filter cứng sang cơ chế “scoring” duy nhất
   - Vì sao: Xếp hạng linh hoạt, giảm thủ công “ưu tiên tên rồi lại sort”.
   - Làm gì: score = ageOk(>0) + genreMatch(%) + nameMatch(exact/startsWith/contains) + ratingWeight + freshnessWeight.
   - Tiêu chí xong: Một hàm score(movie, ctx) → sort theo điểm, kết quả ổn định.
4. Boost theo lịch chiếu gần (schedule-aware)
   - Vì sao: Trả về phim “xem được ngay”, sát ngữ cảnh hiện tại.
   - Làm gì: hasShowtimeIn(nextDays=7) → +weight lớn; nếu request có ngày/khung giờ → boost theo khung đó.
   - Tiêu chí xong: Top kết quả đa phần có suất chiếu khả dụng.
5. Giới hạn top-N trước khi gọi AI + guardrails prompt
   - Vì sao: Tiết kiệm chi phí, giảm “bịa”.
   - Làm gì: Cắt N=5–7; System prompt: “Chỉ dùng dữ liệu cung cấp, không bịa, nếu thiếu thì nói ‘chưa tìm thấy’ + gợi ý thay đổi tiêu chí”. Temperature 0–0.3.
   - Tiêu chí xong: Prompt ngắn gọn, câu trả lời bám đúng danh sách đã lọc.
6. Logging lý do loại & điểm số
   - Vì sao: Debug và tune dễ; hiểu vì sao phim A rớt, phim B lên top.
   - Làm gì: Log flags ageFail|genreScore|nameScore|scheduleBoost|finalScore cho top 20.
   - Tiêu chí xong: Có log đọc được, reproducible.
