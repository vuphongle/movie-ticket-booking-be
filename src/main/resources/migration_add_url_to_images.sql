-- Migration script để thêm cột url vào bảng images
-- Chạy script này để cập nhật database schema

ALTER TABLE images ADD COLUMN url VARCHAR(500);

-- Cập nhật các record hiện tại với URL pattern cũ (nếu có)
-- UPDATE images SET url = CONCAT('/api/public/images/', id) WHERE url IS NULL;

-- Lưu ý: Các ảnh đã upload sẽ cần được migrate từ local storage lên S3
-- hoặc cập nhật URL thủ công tương ứng với S3 URLs.