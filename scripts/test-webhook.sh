#!/bin/bash

# PayOS Webhook Test Script
# Usage: ./test-webhook.sh [environment]
# Example: ./test-webhook.sh local
#          ./test-webhook.sh production

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${BLUE}ℹ ${1}${NC}"
}

print_success() {
    echo -e "${GREEN}✓ ${1}${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ ${1}${NC}"
}

print_error() {
    echo -e "${RED}✗ ${1}${NC}"
}

# Determine environment
ENV=${1:-local}

if [ "$ENV" == "local" ]; then
    # Check if .env exists
    if [ ! -f .env ]; then
        print_error ".env file not found!"
        print_info "Please copy .env.example to .env and configure it"
        exit 1
    fi
    
    # Load .env
    source .env
    
    WEBHOOK_URL=${PAYOS_WEBHOOK_URL:-http://localhost:8080/api/payos-webhook}
    print_info "Testing local webhook: $WEBHOOK_URL"
    
elif [ "$ENV" == "production" ] || [ "$ENV" == "prod" ]; then
    print_info "Testing production webhook"
    read -p "Enter production webhook URL: " WEBHOOK_URL
else
    WEBHOOK_URL=$ENV
    print_info "Testing custom webhook URL: $WEBHOOK_URL"
fi

echo ""
print_info "=== PayOS Webhook Test ==="
echo ""

# Test 1: Check if endpoint is accessible
print_info "Test 1: Checking endpoint accessibility..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$WEBHOOK_URL" || echo "000")

if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "405" ]; then
    print_success "Endpoint is accessible (HTTP $HTTP_CODE)"
elif [ "$HTTP_CODE" == "401" ]; then
    print_warning "Endpoint requires authentication (HTTP 401) - This is expected"
else
    print_error "Endpoint not accessible (HTTP $HTTP_CODE)"
    print_info "Please ensure the backend is running"
    exit 1
fi

echo ""

# Test 2: Test webhook with sample data (will fail signature verification - that's expected)
print_info "Test 2: Sending test webhook request..."

TEST_PAYLOAD='{
  "code": "00",
  "desc": "success",
  "data": {
    "orderCode": 12345,
    "amount": 50000,
    "description": "ThanhToanVe 12345",
    "accountNumber": "12345678",
    "reference": "REF12345",
    "transactionDateTime": "2025-10-21T10:00:00Z",
    "status": "PAID"
  }
}'

RESPONSE=$(curl -s -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -H "x-payos-signature: test-signature-will-fail" \
  -d "$TEST_PAYLOAD")

echo "Response: $RESPONSE"

if echo "$RESPONSE" | grep -q "signature"; then
    print_success "Webhook endpoint is processing requests (signature verification working)"
elif echo "$RESPONSE" | grep -q "error"; then
    print_warning "Webhook returned error - check logs for details"
else
    print_info "Unexpected response - check backend logs"
fi

echo ""

# Test 3: Check backend logs (if running locally)
if [ "$ENV" == "local" ]; then
    print_info "Test 3: Checking backend logs..."
    
    if [ -d "logs" ]; then
        echo ""
        print_info "Recent webhook logs:"
        if [ -f "logs/application.log" ]; then
            tail -20 logs/application.log | grep -i webhook || print_warning "No webhook logs found"
        else
            print_warning "Log file not found at logs/application.log"
        fi
    elif command -v docker-compose &> /dev/null; then
        print_info "Checking Docker logs..."
        docker-compose logs --tail=20 backend | grep -i webhook || print_warning "No webhook logs in Docker"
    else
        print_warning "Could not find logs. Please check manually."
    fi
fi

echo ""
print_info "=== Test Summary ==="
echo ""
print_info "Next steps:"
echo "  1. Check backend logs for webhook processing details"
echo "  2. Configure webhook URL on PayOS dashboard: $WEBHOOK_URL"
echo "  3. Test with real payment and close browser tab"
echo "  4. Verify order status updates correctly"
echo ""
print_success "Test completed!"
