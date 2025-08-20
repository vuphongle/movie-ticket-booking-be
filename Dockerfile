# Multi-stage build để tối ưu kích thước image

# Stage 1: Build stage với JDK đầy đủ
FROM eclipse-temurin:17-jdk AS builder

# Thông tin metadata cho build stage
LABEL stage=builder

# Tạo thư mục làm việc
WORKDIR /app

# Copy Gradle wrapper và build files
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

# Cấp quyền thực thi cho gradlew
RUN chmod +x ./gradlew

# Download dependencies trước (cache layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build application (skip tests để tăng tốc độ build)
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime stage với distroless image (siêu nhỏ gọn)
FROM gcr.io/distroless/java17-debian12:nonroot AS runtime

# Thông tin metadata
LABEL maintainer="vuphong <email@example.com>"
LABEL description="Movie Ticket Booking Backend API"
LABEL version="1.0"

# Tạo thư mục làm việc
WORKDIR /app

# Copy JAR file từ build stage
COPY --from=builder /app/build/libs/movie-ticket-booking-be-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Environment variables mặc định (sẽ được override trong production)
ENV SPRING_PROFILES_ACTIVE=docker \
    DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=db-movie-ticket-booking \
    DB_USERNAME=movie_user \
    DB_PASSWORD=change_me_password \
    JWT_SECRET=change_me_jwt_secret_key_minimum_256_bits \
    MAIL_USERNAME=your_email@gmail.com \
    MAIL_PASSWORD=your_app_password \
    FRONTEND_HOST=http://localhost \
    FRONTEND_PORT=3000 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication" \
    TZ=Asia/Ho_Chi_Minh

CMD ["/app/app.jar"]