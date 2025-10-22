# 📚 Documentation Index - Movie Ticket Booking Backend

## 📋 Tổng quan

Đây là hệ thống đặt vé xem phim trực tuyến được xây dựng với Spring Boot. Tài liệu này cung cấp hướng dẫn chi tiết về deployment, configuration và troubleshooting.

## 🗂️ Danh mục tài liệu

### 🚀 Deployment & Setup

1. **[Full Deployment Guide](./FULL-DEPLOYMENT-GUIDE.md)**
   - Hướng dẫn deploy toàn bộ hệ thống
   - Cấu hình database, backend, frontend
   - Docker & Docker Compose setup
   - Production best practices

### 💳 Payment Integration

#### PayOS Webhook Implementation (NEW)

2. **[PayOS Webhook Quick Start](./PAYOS-WEBHOOK-QUICKSTART.md)** ⭐ **BẮT ĐẦU ĐÂY**

   - Hướng dẫn nhanh setup webhook trong 5 phút
   - Commands đầy đủ cho local dev và production
   - Checklist và troubleshooting cơ bản
   - **Recommended cho người mới bắt đầu**

3. **[PayOS Webhook Setup Guide](./PAYOS-WEBHOOK-SETUP.md)** 📖 **CHI TIẾT**

   - Hướng dẫn chi tiết về webhook architecture
   - Cấu hình cho từng môi trường (dev/staging/prod)
   - Security best practices
   - Testing và monitoring guide
   - Advanced troubleshooting

4. **[PayOS Webhook Changes](./PAYOS-WEBHOOK-CHANGES.md)** 🔍 **TECHNICAL**
   - Summary về các thay đổi code
   - Danh sách files đã modify
   - API changes và new endpoints
   - Migration guide từ version cũ

### 🤖 AI Features

5. **[Improve Chatbot](./improve%20chatbot.md)**
   - Cải thiện chatbot tích hợp AI
   - OpenAI integration
   - Custom prompts và responses

## 🎯 Use Cases - Chọn tài liệu phù hợp

### Bạn muốn setup webhook cho PayOS?

**→ Bắt đầu với [PayOS Webhook Quick Start](./PAYOS-WEBHOOK-QUICKSTART.md)**

Sau đó đọc thêm:

- [Setup Guide](./PAYOS-WEBHOOK-SETUP.md) cho details
- [Changes Doc](./PAYOS-WEBHOOK-CHANGES.md) để hiểu code changes

### Bạn muốn deploy lên production?

**→ Đọc [Full Deployment Guide](./FULL-DEPLOYMENT-GUIDE.md)**

Lưu ý về PayOS webhook:

- Cần HTTPS domain (không dùng được IP)
- Phải configure webhook URL trên PayOS dashboard
- Test kỹ trước khi go live

### Bạn đang troubleshoot payment issues?

**→ Check [PayOS Webhook Setup Guide](./PAYOS-WEBHOOK-SETUP.md)** - section Troubleshooting

Common issues:

- Order không update khi đóng tab → Webhook chưa configure
- Signature verification failed → Check `PAYOS_CHECKSUM_KEY`
- Webhook không được gọi → Verify URL trên PayOS dashboard

### Bạn muốn improve chatbot?

**→ Xem [Improve Chatbot](./improve%20chatbot.md)**

## 🔧 Quick Commands

### Test Webhook (Local)

```bash
# Start backend
./gradlew bootRun

# In another terminal - start ngrok
ngrok http 8080

# Test webhook endpoint
./scripts/test-webhook.sh local
```

### Deploy Production

```bash
# SSH to VPS
ssh user@your-vps

# Pull latest code
git pull origin main

# Update environment
nano .env  # Add PAYOS_WEBHOOK_URL

# Deploy
docker-compose down
docker-compose up -d --build

# Verify
docker-compose logs -f backend | grep webhook
```

### Check Logs

```bash
# Local development
tail -f logs/application.log | grep -i webhook

# Docker/Production
docker-compose logs -f backend | grep -i webhook

# Nginx (if applicable)
tail -f /var/log/nginx/access.log | grep payos-webhook
```

## 📊 Architecture Overview

```
┌─────────────┐
│   User      │
└──────┬──────┘
       │
       ▼
┌─────────────┐     Payment      ┌──────────────┐
│  Frontend   │ ───────────────► │    PayOS     │
│   (React)   │                   │   Payment    │
└──────┬──────┘                   └──────┬───────┘
       │                                  │
       │ Return URL (Optional)            │ Webhook (Main)
       │                                  │
       ▼                                  ▼
┌──────────────────────────────────────────┐
│          Backend (Spring Boot)            │
│  ┌────────────────────────────────────┐  │
│  │  POST /api/payos-webhook           │  │
│  │  - Verify signature                │  │
│  │  - Parse webhook data              │  │
│  │  - Update order status             │  │
│  └────────────────────────────────────┘  │
└──────────────┬───────────────────────────┘
               │
               ▼
        ┌──────────────┐
        │   Database   │
        │   (MariaDB)  │
        └──────────────┘
```

## 🔐 Security Checklist

- [ ] All PayOS credentials in .env (not committed to git)
- [ ] Webhook signature verification enabled
- [ ] HTTPS enforced for production
- [ ] Database credentials strong and secure
- [ ] JWT secret key properly configured
- [ ] API rate limiting configured
- [ ] Firewall rules properly set

## 📞 Support & Contributing

### Gặp vấn đề?

1. Check tài liệu troubleshooting phù hợp
2. Review logs để identify issue
3. Search existing issues trên GitHub
4. Tạo new issue với đầy đủ thông tin

### Contribute

- Fork repo
- Create feature branch
- Make changes
- Write/update docs
- Submit pull request

## 🔄 Recent Updates

### October 2025

- ✅ **PayOS Webhook Implementation**
  - Fixed issue: Order không update khi user đóng tab
  - Added webhook verification với HMAC-SHA256
  - Full logging và error handling
  - Production-ready với SSL support

### Previous Updates

- Chatbot improvements với OpenAI
- Full deployment guide
- Docker containerization

## 📝 Contributing to Docs

Khi thêm features mới, nhớ update docs:

1. Tạo file mới trong `/docs` nếu cần
2. Update README này với links
3. Thêm vào appropriate use case section
4. Include examples và troubleshooting

## 🎓 Learning Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [PayOS API Docs](https://payos.vn/docs/api/)
- [Docker Documentation](https://docs.docker.com/)
- [Nginx Documentation](https://nginx.org/en/docs/)

---

**Last Updated**: October 21, 2025
**Maintainers**: Development Team

**Need help?** Start with the appropriate quick start guide above! 🚀
