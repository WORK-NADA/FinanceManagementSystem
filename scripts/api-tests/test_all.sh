#!/bin/bash
# =============================================================================
# test_all.sh - Full API Test Suite (curl + jq)
# Run: bash scripts/api-tests/test_all.sh
# Requires: curl, jq, running server on localhost:8080
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/_helpers.sh"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="$SCRIPT_DIR/results"
mkdir -p "$RESULTS_DIR"
LOG_FILE="$RESULTS_DIR/$TIMESTAMP.log"
touch "$LOG_FILE"

log "======================================================================"
log " API TEST SUITE RUN: $TIMESTAMP"
log "======================================================================"
log " Server: $API_URL"
log "======================================================================"

# =============================================================================
# SETUP: Ensure test accounts exist
# =============================================================================
log ""
log "[SETUP] Ensuring test client accounts exist..."
bash "$SCRIPT_DIR/../create-test-client.sh" >> "$LOG_FILE" 2>&1 || true
log "[SETUP] Done."

log ""
log "[SETUP] Logging in as ADMIN, CLIENT1, CLIENT2..."
ADMIN_TOKEN=$(login "$ADMIN_EMAIL" "$ADMIN_PASS")
CLIENT1_TOKEN=$(login "$CLIENT1_EMAIL" "$CLIENT1_PASS")
CLIENT2_TOKEN=$(login "$CLIENT2_EMAIL" "$CLIENT2_PASS")

if [ -z "$ADMIN_TOKEN" ]; then log "[SETUP] ERROR: Could not get ADMIN token. Is the server running?"; exit 1; fi
if [ -z "$CLIENT1_TOKEN" ]; then log "[SETUP] ERROR: Could not get CLIENT1 token."; exit 1; fi
if [ -z "$CLIENT2_TOKEN" ]; then log "[SETUP] ERROR: Could not get CLIENT2 token."; exit 1; fi
log "[SETUP] All tokens acquired."

FAKE_UUID="00000000-0000-4000-8000-000000000001"
# Unique suffix per run to prevent duplicate-key conflicts across test runs
RUN_ID=$(date +%s | tail -c 6)
# 4-digit ID for GST numbers (must follow strict format)
GST_ID=$(printf "%04d" $((RANDOM % 10000)))
# Dynamic year for profit distribution to avoid "already distributed" cross-run conflicts
PD_YEAR=$((2026 + (RANDOM % 50)))

# =============================================================================
# MODULE: Auth
# =============================================================================
M="Auth"
log_header "MODULE: $M (/user/login, /user/refresh-token)"

call_api POST "/user/login" NONE \
  '{"email":"urvip249@gmail.com","password":"urviAK2005!"}' \
  200 "Login valid credentials" "$M"

call_api POST "/user/login" NONE \
  '{"email":"urvip249@gmail.com","password":"wrongpassword"}' \
  400 "Login wrong password" "$M"

call_api POST "/user/login" NONE \
  '{"email":"nobody@nonexistent.example","password":"TestPass@123"}' \
  401 "Login non-existent email" "$M"

call_api POST "/user/login" NONE \
  '{}' \
  400 "Login missing email and password" "$M"

call_api POST "/user/login" NONE \
  '{"email":"notanemail","password":"TestPass@123"}' \
  400 "Login malformed email format" "$M"

call_api POST "/user/login" NONE \
  '{"email":"urvip249@gmail.com"}' \
  400 "Login missing password field" "$M"

# Grab a valid refresh token for testing
ADMIN_REFRESH=$(login_get_refresh "$ADMIN_EMAIL" "$ADMIN_PASS")

call_api POST "/auth/refresh" NONE \
  '{"refreshToken":"garbage.token.here"}' \
  401 "Refresh with garbage token" "$M"

call_api POST "/auth/refresh" NONE \
  '{}' \
  400 "Refresh missing refreshToken field" "$M"

if [ -n "$ADMIN_REFRESH" ]; then
    call_api POST "/auth/refresh" NONE \
      "{\"refreshToken\":\"$ADMIN_REFRESH\"}" \
      200 "Refresh with valid token" "$M"
fi

# =============================================================================
# MODULE: Admin
# =============================================================================
M="Admin"
log_header "MODULE: $M (/admin/register, /admin/users)"

# Role check
call_api POST "/admin/register" "$CLIENT1_TOKEN" \
  '{"email":"x@x.com"}' \
  403 "Register as CLIENT (role check)" "$M"

call_api GET "/admin/users" "$CLIENT1_TOKEN" NONE \
  403 "List users as CLIENT (role check)" "$M"

# No auth
call_api POST "/admin/register" NONE \
  '{"email":"x@x.com"}' \
  401 "Register no auth header" "$M"

call_api GET "/admin/users" NONE NONE \
  401 "List users no auth header" "$M"

# Garbage token
call_api GET "/admin/users" "garbage.jwt.token" NONE \
  401 "List users garbage token" "$M"

# Validation: missing required fields
call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{}' \
  400 "Register empty body" "$M"

call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{"username":"tc_test","email":"tctest@example.com","password":"TestPass@123","mobileNumber":"9876543299","role":"CLIENT","userAddress":{"houseNo":"1","societyName":"S","area":"A","city":"Ahmedabad","pincode":"380001","state":"Gujarat","country":"India"}}' \
  400 "Register missing ownerName" "$M"

call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{"ownerName":"Test User","email":"tctest@example.com","password":"TestPass@123","mobileNumber":"9876543299","role":"CLIENT","userAddress":{"houseNo":"1","societyName":"S","area":"A","city":"Ahmedabad","pincode":"380001","state":"Gujarat","country":"India"}}' \
  400 "Register missing username" "$M"

call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{"ownerName":"Test User","username":"tc_test","password":"TestPass@123","mobileNumber":"9876543299","role":"CLIENT","userAddress":{"houseNo":"1","societyName":"S","area":"A","city":"Ahmedabad","pincode":"380001","state":"Gujarat","country":"India"}}' \
  400 "Register missing email" "$M"

call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{"ownerName":"Test User","username":"tc_test","email":"tctest@example.com","mobileNumber":"9876543299","role":"CLIENT","userAddress":{"houseNo":"1","societyName":"S","area":"A","city":"Ahmedabad","pincode":"380001","state":"Gujarat","country":"India"}}' \
  400 "Register missing password" "$M"

call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{"ownerName":"Test User","username":"tc_test","email":"tctest@example.com","password":"TestPass@123","role":"CLIENT","userAddress":{"houseNo":"1","societyName":"S","area":"A","city":"Ahmedabad","pincode":"380001","state":"Gujarat","country":"India"}}' \
  400 "Register missing mobileNumber" "$M"

call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{"ownerName":"Test User","username":"tc_test","email":"tctest@example.com","password":"TestPass@123","mobileNumber":"9876543299","role":"CLIENT"}' \
  400 "Register missing userAddress" "$M"

# Weak password
call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{"ownerName":"Test User","username":"tc_test2","email":"tct2@example.com","password":"abc123","mobileNumber":"9876543298","role":"CLIENT","userAddress":{"houseNo":"1","societyName":"S","area":"A","city":"Ahmedabad","pincode":"380001","state":"Gujarat","country":"India"}}' \
  400 "Register weak password (no special/uppercase)" "$M"

# Invalid mobile (starts with 5)
call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{"ownerName":"Test User","username":"tc_test3","email":"tct3@example.com","password":"TestPass@123","mobileNumber":"5876543299","role":"CLIENT","userAddress":{"houseNo":"1","societyName":"S","area":"A","city":"Ahmedabad","pincode":"380001","state":"Gujarat","country":"India"}}' \
  400 "Register invalid mobile (starts with 5)" "$M"

# Invalid mobile (9 digits)
call_api POST "/admin/register" "$ADMIN_TOKEN" \
  '{"ownerName":"Test User","username":"tc_test4","email":"tct4@example.com","password":"TestPass@123","mobileNumber":"987654321","role":"CLIENT","userAddress":{"houseNo":"1","societyName":"S","area":"A","city":"Ahmedabad","pincode":"380001","state":"Gujarat","country":"India"}}' \
  400 "Register invalid mobile (9 digits)" "$M"

# Duplicate email (testclient1 already exists)
call_api POST "/admin/register" "$ADMIN_TOKEN" \
  "{\"ownerName\":\"Dup User\",\"username\":\"dup_username_$(date +%s)\",\"email\":\"testclient1@example.com\",\"password\":\"TestPass@123\",\"mobileNumber\":\"9876549999\",\"role\":\"CLIENT\",\"userAddress\":{\"houseNo\":\"1\",\"societyName\":\"Society Test\",\"area\":\"Area Test\",\"city\":\"Ahmedabad\",\"pincode\":\"380001\",\"state\":\"Gujarat\",\"country\":\"India\"}}" \
  409 "Register duplicate email (report actual status)" "$M"
log "       NOTE: Expected 409 for duplicate email - actual status above. Flag if 500."

# Valid registration (new unique user)
TS=$(date +%s)
call_api POST "/admin/register" "$ADMIN_TOKEN" \
  "{\"ownerName\":\"Test Temp\",\"username\":\"testtemp$TS\",\"email\":\"testtemp$TS@example.com\",\"password\":\"TestPass@123\",\"mobileNumber\":\"6666${TS: -6}\",\"role\":\"CLIENT\",\"userAddress\":{\"houseNo\":\"1\",\"societyName\":\"Society Test\",\"area\":\"Area Test\",\"city\":\"Ahmedabad\",\"pincode\":\"380001\",\"state\":\"Gujarat\",\"country\":\"India\"}}" \
  200 "Register valid new user" "$M"

# List users (valid)
call_api GET "/admin/users" "$ADMIN_TOKEN" NONE \
  200 "List users as ADMIN" "$M"

# =============================================================================
# MODULE: Customer
# =============================================================================
M="Customer"
log_header "MODULE: $M (/customer)"

# No auth
call_api GET "/customer/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/customer/all" "garbage.token" NONE 401 "Get all - garbage token" "$M"

# Syntactically invalid path UUID
call_api GET "/customer/abc123" "$ADMIN_TOKEN" NONE 400 "Get by malformed UUID (not UUID)" "$M"
call_api GET "/customer/abc123" "$CLIENT1_TOKEN" NONE 400 "Get by malformed UUID as CLIENT" "$M"

# Non-existent UUID
call_api GET "/customer/$FAKE_UUID" "$ADMIN_TOKEN" NONE 404 "Get non-existent UUID as ADMIN" "$M"
call_api GET "/customer/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent UUID as CLIENT" "$M"

# Add - valid (ADMIN)
call_api POST "/customer/add" "$ADMIN_TOKEN" \
  "{\"customerName\":\"TEST_CustAdmin\",\"mobileNumber\":\"910${RUN_ID}01\",\"email\":\"test.cust.admin${RUN_ID}@example.com\",\"gstNumber\":\"24ABCDE${GST_ID}A1Z5\",\"openingBalance\":0,\"paymentTerms\":30,\"address\":{\"addressLine1\":\"123 Test Road\",\"city\":\"Ahmedabad\",\"state\":\"Gujarat\",\"country\":\"India\",\"pincode\":\"380001\"}}" \
  201 "Add customer valid - ADMIN" "$M"
ADMIN_CUST_ID=$(extract_public_id)

# Add - valid (CLIENT1)
call_api POST "/customer/add" "$CLIENT1_TOKEN" \
  "{\"customerName\":\"TEST_CustClient1\",\"mobileNumber\":\"910${RUN_ID}02\",\"email\":\"test.cust.client1${RUN_ID}@example.com\",\"gstNumber\":\"24ABCDE${GST_ID}C1Z5\",\"openingBalance\":0,\"paymentTerms\":30,\"address\":{\"addressLine1\":\"456 Test Road\",\"city\":\"Surat\",\"state\":\"Gujarat\",\"country\":\"India\",\"pincode\":\"395001\"}}" \
  201 "Add customer valid - CLIENT1" "$M"
C1_CUST_ID=$(extract_public_id)

# Add - valid (CLIENT2 - different user)
call_api POST "/customer/add" "$CLIENT2_TOKEN" \
  "{\"customerName\":\"TEST_CustClient2\",\"mobileNumber\":\"910${RUN_ID}03\",\"email\":\"test.cust.client2${RUN_ID}@example.com\",\"gstNumber\":\"24ABCDE${GST_ID}D1Z5\",\"openingBalance\":0,\"paymentTerms\":30,\"address\":{\"addressLine1\":\"789 Test Road\",\"city\":\"Vadodara\",\"state\":\"Gujarat\",\"country\":\"India\",\"pincode\":\"390001\"}}" \
  201 "Add customer valid - CLIENT2" "$M"
C2_CUST_ID=$(extract_public_id)

# Validation failures
call_api POST "/customer/add" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Add customer empty body" "$M"

call_api POST "/customer/add" "$CLIENT1_TOKEN" \
  '{"mobileNumber":"9100000099","email":"x@test.com","gstNumber":"24ABCDE1234F1Z5","openingBalance":0,"paymentTerms":30,"address":{"addressLine1":"123 Rd","city":"C","state":"S","country":"I","pincode":"380001"}}' \
  400 "Add customer missing customerName" "$M"

call_api POST "/customer/add" "$CLIENT1_TOKEN" \
  '{"customerName":"TEST_Cust","email":"x@test.com","gstNumber":"24ABCDE1234F1Z5","openingBalance":0,"paymentTerms":30,"address":{"addressLine1":"123 Rd","city":"C","state":"S","country":"I","pincode":"380001"}}' \
  400 "Add customer missing mobileNumber" "$M"

call_api POST "/customer/add" "$CLIENT1_TOKEN" \
  '{"customerName":"TEST_Cust","mobileNumber":"5000000000","email":"x@test.com","gstNumber":"24ABCDE1234F1Z5","openingBalance":0,"paymentTerms":30,"address":{"addressLine1":"123 Rd","city":"C","state":"S","country":"I","pincode":"380001"}}' \
  400 "Add customer invalid mobile (starts with 5)" "$M"

call_api POST "/customer/add" "$CLIENT1_TOKEN" \
  '{"customerName":"TEST_Cust","mobileNumber":"9100000099","email":"x@test.com","gstNumber":"INVALID_GST","openingBalance":0,"paymentTerms":30,"address":{"addressLine1":"123 Rd","city":"C","state":"S","country":"I","pincode":"380001"}}' \
  400 "Add customer invalid GST format" "$M"

call_api POST "/customer/add" "$CLIENT1_TOKEN" \
  '{"customerName":"TEST_Cust","mobileNumber":"9100000099","email":"x@test.com","gstNumber":"24ABCDE1234F1Z5","openingBalance":0,"paymentTerms":30,"address":{"addressLine1":"123 Rd","city":"C","state":"S","country":"I","pincode":"12345"}}' \
  400 "Add customer invalid pincode (5 digits)" "$M"

call_api POST "/customer/add" "$CLIENT1_TOKEN" \
  '{"customerName":"TEST_Cust","mobileNumber":"9100000099","email":"bademail","gstNumber":"24ABCDE1234F1Z5","openingBalance":0,"paymentTerms":30,"address":{"addressLine1":"123 Rd","city":"Ahmedabad","state":"Gujarat","country":"India","pincode":"380001"}}' \
  400 "Add customer invalid email format" "$M"

# Duplicate mobile for same user
call_api POST "/customer/add" "$CLIENT1_TOKEN" \
  "{\"customerName\":\"TEST_CustDup\",\"mobileNumber\":\"910${RUN_ID}02\",\"email\":\"dup${RUN_ID}@test.com\",\"gstNumber\":\"27AAPFU0939F1ZV\",\"openingBalance\":0,\"paymentTerms\":30,\"address\":{\"addressLine1\":\"123 Rd\",\"city\":\"Ahmedabad\",\"state\":\"Gujarat\",\"country\":\"India\",\"pincode\":\"380001\"}}" \
  409 "Add customer duplicate mobile (same user) - report actual status" "$M"
log "       NOTE: Expected 409 for duplicate mobile - flag if 500."

# Get by valid ID (happy path)
if [ -n "$C1_CUST_ID" ] && [ "$C1_CUST_ID" != "null" ]; then
    call_api GET "/customer/$C1_CUST_ID" "$CLIENT1_TOKEN" NONE 200 "Get customer own record - CLIENT1" "$M"
fi

# Data isolation: CLIENT2 reads CLIENT1's customer
if [ -n "$C1_CUST_ID" ] && [ "$C1_CUST_ID" != "null" ]; then
    call_api GET "/customer/$C1_CUST_ID" "$CLIENT2_TOKEN" NONE 404 "Isolation: CLIENT2 reads CLIENT1 customer" "$M"
fi

# Isolation on update
if [ -n "$C1_CUST_ID" ] && [ "$C1_CUST_ID" != "null" ]; then
    call_api PUT "/customer/$C1_CUST_ID" "$CLIENT2_TOKEN" \
      '{"customerName":"HACK","mobileNumber":"9100000002","email":"hack@test.com","gstNumber":"24ABCDE1234F1Z5","openingBalance":0,"paymentTerms":30,"address":{"addressLine1":"hack","city":"CityTest","state":"StateTest","country":"CountryTest","pincode":"380001"}}' \
      404 "Isolation: CLIENT2 updates CLIENT1 customer" "$M"
fi

# Deactivate and reactivate (CLIENT1 own)
if [ -n "$C1_CUST_ID" ] && [ "$C1_CUST_ID" != "null" ]; then
    call_api PATCH "/customer/$C1_CUST_ID/deactivate" "$CLIENT1_TOKEN" NONE 200 "Deactivate own customer - CLIENT1" "$M"
    call_api PATCH "/customer/$C1_CUST_ID/deactivate" "$CLIENT1_TOKEN" NONE 200 "Deactivate already-inactive customer (idempotent check)" "$M"
    call_api PATCH "/customer/$C1_CUST_ID/reactivate" "$CLIENT1_TOKEN" NONE 200 "Reactivate customer - CLIENT1" "$M"
    call_api PATCH "/customer/$C1_CUST_ID/reactivate" "$CLIENT1_TOKEN" NONE 200 "Reactivate already-active customer (idempotent check)" "$M"
fi

# =============================================================================
# MODULE: Supplier
# =============================================================================
M="Supplier"
log_header "MODULE: $M (/supplier)"

call_api GET "/supplier/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/supplier/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent UUID" "$M"
call_api GET "/supplier/abc123" "$CLIENT1_TOKEN" NONE 400 "Get malformed UUID" "$M"

# Add valid - ADMIN
call_api POST "/supplier/add" "$ADMIN_TOKEN" \
  "{\"supplierName\":\"TEST_SupplierAdmin\",\"mobileNumber\":\"920${RUN_ID}01\",\"email\":\"test.sup.admin${RUN_ID}@example.com\",\"gstNumber\":\"24ABCDE${GST_ID}E1Z5\"}" \
  201 "Add supplier valid - ADMIN" "$M"
ADMIN_SUPP_ID=$(extract_public_id)

# Add valid - CLIENT1
call_api POST "/supplier/add" "$CLIENT1_TOKEN" \
  "{\"supplierName\":\"TEST_SupplierClient1\",\"mobileNumber\":\"920${RUN_ID}02\",\"email\":\"test.sup.c1${RUN_ID}@example.com\",\"gstNumber\":\"24ABCDE${GST_ID}F1Z5\"}" \
  201 "Add supplier valid - CLIENT1" "$M"
C1_SUPP_ID=$(extract_public_id)

# Add valid - CLIENT2
call_api POST "/supplier/add" "$CLIENT2_TOKEN" \
  "{\"supplierName\":\"TEST_SupplierClient2\",\"mobileNumber\":\"920${RUN_ID}03\",\"email\":\"test.sup.c2${RUN_ID}@example.com\"}" \
  201 "Add supplier valid - CLIENT2" "$M"
C2_SUPP_ID=$(extract_public_id)

# Validation failures
call_api POST "/supplier/add" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Add supplier empty body" "$M"

call_api POST "/supplier/add" "$CLIENT1_TOKEN" \
  '{"mobileNumber":"9200000099","email":"sup@test.com"}' \
  400 "Add supplier missing supplierName" "$M"

call_api POST "/supplier/add" "$CLIENT1_TOKEN" \
  '{"supplierName":"TEST_Sup","email":"sup@test.com"}' \
  400 "Add supplier missing mobileNumber" "$M"

call_api POST "/supplier/add" "$CLIENT1_TOKEN" \
  '{"supplierName":"TEST_Sup","mobileNumber":"1234567890","email":"sup@test.com"}' \
  400 "Add supplier invalid mobile (10 digits but starts with 1)" "$M"

call_api POST "/supplier/add" "$CLIENT1_TOKEN" \
  '{"supplierName":"TEST_Sup","mobileNumber":"9200009999","gstNumber":"BADGST"}' \
  400 "Add supplier invalid GST format" "$M"

# Duplicate mobile same user
call_api POST "/supplier/add" "$CLIENT1_TOKEN" \
  "{\"supplierName\":\"TEST_SupDup\",\"mobileNumber\":\"920${RUN_ID}02\",\"email\":\"sup.dup${RUN_ID}@test.com\"}" \
  409 "Add supplier duplicate mobile (same user) - report actual" "$M"
log "       NOTE: Expected 409 for duplicate mobile - flag if 500."

# Isolation checks
if [ -n "$C1_SUPP_ID" ] && [ "$C1_SUPP_ID" != "null" ]; then
    call_api GET "/supplier/$C1_SUPP_ID" "$CLIENT2_TOKEN" NONE 404 "Isolation: CLIENT2 reads CLIENT1 supplier" "$M"
    call_api PUT "/supplier/$C1_SUPP_ID" "$CLIENT2_TOKEN" \
      '{"supplierName":"HACK","mobileNumber":"9200000002"}' \
      404 "Isolation: CLIENT2 updates CLIENT1 supplier" "$M"
fi

# Deactivate/reactivate
if [ -n "$C1_SUPP_ID" ] && [ "$C1_SUPP_ID" != "null" ]; then
    call_api PATCH "/supplier/$C1_SUPP_ID/deactivate" "$CLIENT1_TOKEN" NONE 200 "Deactivate own supplier - CLIENT1" "$M"
    call_api PATCH "/supplier/$C1_SUPP_ID/deactivate" "$CLIENT1_TOKEN" NONE 200 "Deactivate already-inactive supplier (idempotent)" "$M"
    call_api PATCH "/supplier/$C1_SUPP_ID/reactivate" "$CLIENT1_TOKEN" NONE 200 "Reactivate supplier - CLIENT1" "$M"
fi

# =============================================================================
# MODULE: Stock
# =============================================================================
M="Stock"
log_header "MODULE: $M (/stock)"

call_api GET "/stock/all" NONE NONE 401 "Get all stocks - no auth" "$M"
call_api GET "/stock/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent stock UUID" "$M"
call_api GET "/stock/abc123" "$CLIENT1_TOKEN" NONE 400 "Get stock malformed UUID" "$M"

# Add valid - ADMIN
call_api POST "/stock/add" "$ADMIN_TOKEN" \
  "{\"rawMaterial\":\"TEST_Cotton${RUN_ID}\",\"unit\":\"KG\",\"minimumStockLevel\":100}" \
  201 "Add stock valid - ADMIN" "$M"
ADMIN_STOCK_ID=$(extract_public_id)

# Add valid - CLIENT1
call_api POST "/stock/add" "$CLIENT1_TOKEN" \
  "{\"rawMaterial\":\"TEST_Wheat${RUN_ID}\",\"unit\":\"KG\",\"minimumStockLevel\":50}" \
  201 "Add stock valid - CLIENT1" "$M"
C1_STOCK_ID=$(extract_public_id)

# Add valid - CLIENT2
call_api POST "/stock/add" "$CLIENT2_TOKEN" \
  "{\"rawMaterial\":\"TEST_Rice${RUN_ID}\",\"unit\":\"KG\",\"minimumStockLevel\":200}" \
  201 "Add stock valid - CLIENT2" "$M"
C2_STOCK_ID=$(extract_public_id)

# Validation failures
call_api POST "/stock/add" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Add stock empty body" "$M"

call_api POST "/stock/add" "$CLIENT1_TOKEN" \
  '{"unit":"KG","minimumStockLevel":100}' \
  400 "Add stock missing rawMaterial" "$M"

call_api POST "/stock/add" "$CLIENT1_TOKEN" \
  '{"rawMaterial":"TEST_Material","minimumStockLevel":100}' \
  400 "Add stock missing unit" "$M"

call_api POST "/stock/add" "$CLIENT1_TOKEN" \
  '{"rawMaterial":"TEST_Material2","unit":"LB","minimumStockLevel":100}' \
  400 "Add stock invalid unit enum (LB not in G|KG|TON)" "$M"

call_api POST "/stock/add" "$CLIENT1_TOKEN" \
  '{"rawMaterial":"TEST_Material3","unit":"KG"}' \
  400 "Add stock missing minimumStockLevel" "$M"

call_api POST "/stock/add" "$CLIENT1_TOKEN" \
  '{"rawMaterial":"TEST_Material4","unit":"KG","minimumStockLevel":-10}' \
  400 "Add stock negative minimumStockLevel" "$M"

# Search - valid with param
call_api GET "/stock/search?rawMaterial=TEST" "$CLIENT1_TOKEN" NONE 200 "Search stock with param" "$M"

# Search - empty param (Spring will 400 if @RequestParam is required with no default)
call_api GET "/stock/search" "$CLIENT1_TOKEN" NONE 400 "Search stock missing rawMaterial param" "$M"

# Search - empty string param
call_api GET "/stock/search?rawMaterial=" "$CLIENT1_TOKEN" NONE 200 "Search stock empty string param (graceful behavior check)" "$M"
log "       NOTE: Empty search should return empty list or 400 - check above for 500."

# Isolation
if [ -n "$C1_STOCK_ID" ] && [ "$C1_STOCK_ID" != "null" ]; then
    call_api GET "/stock/$C1_STOCK_ID" "$CLIENT2_TOKEN" NONE 404 "Isolation: CLIENT2 reads CLIENT1 stock" "$M"
fi

# Deactivate/activate
if [ -n "$C1_STOCK_ID" ] && [ "$C1_STOCK_ID" != "null" ]; then
    call_api PATCH "/stock/$C1_STOCK_ID/deactivate" "$CLIENT1_TOKEN" NONE 200 "Deactivate own stock - CLIENT1" "$M"
    call_api PATCH "/stock/$C1_STOCK_ID/activate" "$CLIENT1_TOKEN" NONE 200 "Activate stock - CLIENT1" "$M"
fi

# =============================================================================
# MODULE: StockTransaction
# =============================================================================
M="StockTransaction"
log_header "MODULE: $M (/stock-transaction)"

call_api GET "/stock-transaction/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/stock-transaction/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent transaction UUID" "$M"
call_api GET "/stock-transaction/abc123" "$CLIENT1_TOKEN" NONE 400 "Get malformed UUID" "$M"

# Add valid adjustment IN (CLIENT1)
if [ -n "$C1_STOCK_ID" ] && [ "$C1_STOCK_ID" != "null" ]; then
    call_api POST "/stock-transaction/adjustment" "$CLIENT1_TOKEN" \
      "{\"stockPublicId\":\"$C1_STOCK_ID\",\"transactionType\":\"ADJUSTMENT_IN\",\"quantity\":100,\"remarks\":\"TEST initial stock\"}" \
      201 "Create adjustment IN - CLIENT1" "$M"
    C1_TXN_ID=$(extract_public_id)

    # Add valid adjustment OUT (smaller than available)
    call_api POST "/stock-transaction/adjustment" "$CLIENT1_TOKEN" \
      "{\"stockPublicId\":\"$C1_STOCK_ID\",\"transactionType\":\"ADJUSTMENT_OUT\",\"quantity\":10,\"remarks\":\"TEST small adjustment out\"}" \
      201 "Create adjustment OUT (within stock) - CLIENT1" "$M"

    # ADJUSTMENT_OUT larger than available stock
    call_api POST "/stock-transaction/adjustment" "$CLIENT1_TOKEN" \
      "{\"stockPublicId\":\"$C1_STOCK_ID\",\"transactionType\":\"ADJUSTMENT_OUT\",\"quantity\":99999,\"remarks\":\"TEST overdraft\"}" \
      409 "Adjustment OUT exceeds available stock (should be rejected)" "$M"
    log "       NOTE: Expecting 409 for INSUFFICIENT_STOCK."
fi

# Admin - for its own stock
if [ -n "$ADMIN_STOCK_ID" ] && [ "$ADMIN_STOCK_ID" != "null" ]; then
    call_api POST "/stock-transaction/adjustment" "$ADMIN_TOKEN" \
      "{\"stockPublicId\":\"$ADMIN_STOCK_ID\",\"transactionType\":\"ADJUSTMENT_IN\",\"quantity\":500,\"remarks\":\"TEST admin initial stock\"}" \
      201 "Create adjustment IN - ADMIN" "$M"
fi

# Validation: missing stockPublicId
if [ -n "$C1_STOCK_ID" ] && [ "$C1_STOCK_ID" != "null" ]; then
    call_api POST "/stock-transaction/adjustment" "$CLIENT1_TOKEN" \
      '{"transactionType":"ADJUSTMENT_IN","quantity":10}' \
      400 "Adjustment missing stockPublicId" "$M"

    # Zero quantity (violates > 0 constraint)
    call_api POST "/stock-transaction/adjustment" "$CLIENT1_TOKEN" \
      "{\"stockPublicId\":\"$C1_STOCK_ID\",\"transactionType\":\"ADJUSTMENT_IN\",\"quantity\":0}" \
      400 "Adjustment zero quantity" "$M"

    # Negative quantity
    call_api POST "/stock-transaction/adjustment" "$CLIENT1_TOKEN" \
      "{\"stockPublicId\":\"$C1_STOCK_ID\",\"transactionType\":\"ADJUSTMENT_IN\",\"quantity\":-5}" \
      400 "Adjustment negative quantity" "$M"

    # Invalid transaction type enum
    call_api POST "/stock-transaction/adjustment" "$CLIENT1_TOKEN" \
      "{\"stockPublicId\":\"$C1_STOCK_ID\",\"transactionType\":\"BOGUS_TYPE\",\"quantity\":10}" \
      400 "Adjustment invalid transaction type enum" "$M"
fi

# Non-existent stockPublicId
call_api POST "/stock-transaction/adjustment" "$CLIENT1_TOKEN" \
  "{\"stockPublicId\":\"$FAKE_UUID\",\"transactionType\":\"ADJUSTMENT_IN\",\"quantity\":10}" \
  404 "Adjustment non-existent stockPublicId" "$M"

# Isolation: CLIENT2 adjusts CLIENT1's stock
if [ -n "$C1_STOCK_ID" ] && [ "$C1_STOCK_ID" != "null" ]; then
    call_api POST "/stock-transaction/adjustment" "$CLIENT2_TOKEN" \
      "{\"stockPublicId\":\"$C1_STOCK_ID\",\"transactionType\":\"ADJUSTMENT_IN\",\"quantity\":10}" \
      404 "Isolation: CLIENT2 adjusts CLIENT1 stock" "$M"
fi

# Date range with reversed dates
call_api GET "/stock-transaction/date-range?fromDate=2026-12-31T00:00:00&toDate=2026-01-01T00:00:00" "$CLIENT1_TOKEN" NONE \
  200 "Stock tx date-range reversed (graceful - empty list check)" "$M"
log "       NOTE: Reversed date range should return empty list or 400, not 500."

# =============================================================================
# MODULE: Purchase
# =============================================================================
M="Purchase"
log_header "MODULE: $M (/purchase)"

call_api GET "/purchase/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/purchase/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent purchase" "$M"
call_api GET "/purchase/abc123" "$CLIENT1_TOKEN" NONE 400 "Get malformed UUID" "$M"

# Add valid purchase - CLIENT1 (using CLIENT1's supplier)
if [ -n "$C1_SUPP_ID" ] && [ "$C1_SUPP_ID" != "null" ]; then
    call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
      "{\"supplierPublicId\":\"$C1_SUPP_ID\",\"rawMaterial\":\"TEST_Cotton\",\"weight\":100,\"unit\":\"KG\",\"ratePerUnit\":50,\"gstPercentage\":5,\"purchaseDate\":\"2026-09-01\"}" \
      201 "Add purchase valid - CLIENT1" "$M"
    C1_PURCH_ID=$(extract_public_id)
fi

# Add valid purchase - ADMIN
if [ -n "$ADMIN_SUPP_ID" ] && [ "$ADMIN_SUPP_ID" != "null" ]; then
    call_api POST "/purchase/add" "$ADMIN_TOKEN" \
      "{\"supplierPublicId\":\"$ADMIN_SUPP_ID\",\"rawMaterial\":\"TEST_Cotton${RUN_ID}\",\"weight\":500,\"unit\":\"KG\",\"ratePerUnit\":100,\"gstPercentage\":18,\"purchaseDate\":\"2026-09-01\"}" \
      201 "Add purchase valid - ADMIN" "$M"
    ADMIN_PURCH_ID=$(extract_public_id)
fi

# Validation failures
call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Add purchase empty body" "$M"

call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
  '{"rawMaterial":"TEST_M","weight":100,"unit":"KG","ratePerUnit":50,"gstPercentage":5,"purchaseDate":"2026-09-01"}' \
  400 "Add purchase missing supplierPublicId" "$M"

call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
  "{\"supplierPublicId\":\"$FAKE_UUID\",\"rawMaterial\":\"TEST_M\",\"weight\":100,\"unit\":\"KG\",\"ratePerUnit\":50,\"gstPercentage\":5,\"purchaseDate\":\"2026-09-01\"}" \
  404 "Add purchase non-existent supplierPublicId" "$M"

# Isolation: CLIENT2's supplier used in CLIENT1's purchase request
if [ -n "$C2_SUPP_ID" ] && [ "$C2_SUPP_ID" != "null" ]; then
    call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
      "{\"supplierPublicId\":\"$C2_SUPP_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":100,\"unit\":\"KG\",\"ratePerUnit\":50,\"gstPercentage\":5,\"purchaseDate\":\"2026-09-01\"}" \
      404 "Add purchase using another CLIENT's supplier" "$M"
fi

# weight <= 0
if [ -n "$C1_SUPP_ID" ] && [ "$C1_SUPP_ID" != "null" ]; then
    call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
      "{\"supplierPublicId\":\"$C1_SUPP_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":0,\"unit\":\"KG\",\"ratePerUnit\":50,\"gstPercentage\":5,\"purchaseDate\":\"2026-09-01\"}" \
      400 "Add purchase weight=0" "$M"

    call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
      "{\"supplierPublicId\":\"$C1_SUPP_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":-1,\"unit\":\"KG\",\"ratePerUnit\":50,\"gstPercentage\":5,\"purchaseDate\":\"2026-09-01\"}" \
      400 "Add purchase negative weight" "$M"

    # ratePerUnit <= 0
    call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
      "{\"supplierPublicId\":\"$C1_SUPP_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":100,\"unit\":\"KG\",\"ratePerUnit\":0,\"gstPercentage\":5,\"purchaseDate\":\"2026-09-01\"}" \
      400 "Add purchase ratePerUnit=0" "$M"

    # gstPercentage > 100
    call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
      "{\"supplierPublicId\":\"$C1_SUPP_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":100,\"unit\":\"KG\",\"ratePerUnit\":50,\"gstPercentage\":101,\"purchaseDate\":\"2026-09-01\"}" \
      400 "Add purchase gstPercentage > 100" "$M"

    # gstPercentage < 0
    call_api POST "/purchase/add" "$CLIENT1_TOKEN" \
      "{\"supplierPublicId\":\"$C1_SUPP_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":100,\"unit\":\"KG\",\"ratePerUnit\":50,\"gstPercentage\":-1,\"purchaseDate\":\"2026-09-01\"}" \
      400 "Add purchase gstPercentage < 0" "$M"
fi

# Isolation: CLIENT2 reads CLIENT1's purchase
if [ -n "$C1_PURCH_ID" ] && [ "$C1_PURCH_ID" != "null" ]; then
    call_api GET "/purchase/$C1_PURCH_ID" "$CLIENT2_TOKEN" NONE 404 "Isolation: CLIENT2 reads CLIENT1 purchase" "$M"
fi

# Date range (reversed)
call_api GET "/purchase/date-range?fromDate=2026-12-31&toDate=2026-01-01" "$CLIENT1_TOKEN" NONE \
  200 "Purchase date range reversed (graceful - empty list or 400, not 500)" "$M"
log "       NOTE: Reversed purchase date range - check if 500."

# =============================================================================
# MODULE: PurchasePayment
# =============================================================================
M="PurchasePayment"
log_header "MODULE: $M (/purchase-payment)"

call_api GET "/purchase-payment/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/purchase-payment/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent payment" "$M"

# Add valid payment (CLIENT1)
if [ -n "$C1_PURCH_ID" ] && [ "$C1_PURCH_ID" != "null" ]; then
    call_api POST "/purchase-payment/add" "$CLIENT1_TOKEN" \
      "{\"purchasePublicId\":\"$C1_PURCH_ID\",\"amountPaid\":100,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\",\"remarks\":\"TEST partial payment\"}" \
      201 "Add purchase payment valid - CLIENT1" "$M"
    C1_PP_ID=$(extract_public_id)
    # Verify paymentNumber is system-generated and present
    C1_PP_NUM=$(extract_field "paymentNumber")
    if [ -n "$C1_PP_NUM" ] && [ "$C1_PP_NUM" != "null" ]; then
        log "[PASS] [$M] paymentNumber present in response -> $C1_PP_NUM"
        MODULE_PASS[$M]=$(( ${MODULE_PASS[$M]:-0} + 1 ))
    else
        log "[FAIL] [$M] paymentNumber missing or null in creation response"
        MODULE_FAIL[$M]=$(( ${MODULE_FAIL[$M]:-0} + 1 ))
    fi
    # Verify optional referenceNumber can be provided
    if [ -n "$C1_PURCH_ID" ] && [ "$C1_PURCH_ID" != "null" ]; then
        call_api POST "/purchase-payment/add" "$CLIENT1_TOKEN" \
          "{\"purchasePublicId\":\"$C1_PURCH_ID\",\"amountPaid\":50,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CHEQUE\",\"referenceNumber\":\"CHQ-TEST-$RANDOM\",\"remarks\":\"TEST with referenceNumber\"}" \
          201 "Add purchase payment with optional referenceNumber" "$M"
    fi
fi

# Validation failures
call_api POST "/purchase-payment/add" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Add payment empty body" "$M"

call_api POST "/purchase-payment/add" "$CLIENT1_TOKEN" \
  '{"amountPaid":100,"paymentDate":"2026-09-01","paymentMode":"CASH"}' \
  400 "Add payment missing purchasePublicId" "$M"

call_api POST "/purchase-payment/add" "$CLIENT1_TOKEN" \
  "{\"purchasePublicId\":\"$FAKE_UUID\",\"amountPaid\":100,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\"}" \
  404 "Add payment non-existent purchasePublicId" "$M"

# amountPaid <= 0
if [ -n "$C1_PURCH_ID" ] && [ "$C1_PURCH_ID" != "null" ]; then
    call_api POST "/purchase-payment/add" "$CLIENT1_TOKEN" \
      "{\"purchasePublicId\":\"$C1_PURCH_ID\",\"amountPaid\":0,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\"}" \
      400 "Add payment amountPaid=0" "$M"

    call_api POST "/purchase-payment/add" "$CLIENT1_TOKEN" \
      "{\"purchasePublicId\":\"$C1_PURCH_ID\",\"amountPaid\":-50,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\"}" \
      400 "Add payment negative amountPaid" "$M"

    # Invalid paymentMode enum
    call_api POST "/purchase-payment/add" "$CLIENT1_TOKEN" \
      "{\"purchasePublicId\":\"$C1_PURCH_ID\",\"amountPaid\":50,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"BITCOIN\"}" \
      400 "Add payment invalid paymentMode enum" "$M"

    # Overpayment test (pay way more than the purchase amount)
    call_api POST "/purchase-payment/add" "$CLIENT1_TOKEN" \
      "{\"purchasePublicId\":\"$C1_PURCH_ID\",\"amountPaid\":9999999,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\",\"remarks\":\"TEST overpayment\"}" \
      400 "Add payment overpayment (exceeds total purchase amount)" "$M"
    log "       NOTE: Overpayment - expected 400 (rejected). Flag if 200 (allowed) or 500. This is an ambiguous behavior decision."
fi

# Isolation: CLIENT2 pays CLIENT1's purchase
if [ -n "$C1_PURCH_ID" ] && [ "$C1_PURCH_ID" != "null" ]; then
    call_api POST "/purchase-payment/add" "$CLIENT2_TOKEN" \
      "{\"purchasePublicId\":\"$C1_PURCH_ID\",\"amountPaid\":50,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\"}" \
      404 "Isolation: CLIENT2 pays CLIENT1 purchase" "$M"
fi

# ADMIN uses its own purchase
if [ -n "$ADMIN_PURCH_ID" ] && [ "$ADMIN_PURCH_ID" != "null" ]; then
    call_api POST "/purchase-payment/add" "$ADMIN_TOKEN" \
      "{\"purchasePublicId\":\"$ADMIN_PURCH_ID\",\"amountPaid\":500,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"BANK_TRANSFER\",\"remarks\":\"TEST admin payment\"}" \
      201 "Add purchase payment valid - ADMIN" "$M"
fi

# =============================================================================
# MODULE: Sale
# =============================================================================
M="Sale"
log_header "MODULE: $M (/sale)"

call_api GET "/sale/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/sale/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent sale" "$M"
call_api GET "/sale/abc123" "$CLIENT1_TOKEN" NONE 400 "Get malformed UUID" "$M"

# Add valid sale - CLIENT1 (using CLIENT1's customer)
if [ -n "$C1_CUST_ID" ] && [ "$C1_CUST_ID" != "null" ]; then
    call_api POST "/sale/add" "$CLIENT1_TOKEN" \
      "{\"customerPublicId\":\"$C1_CUST_ID\",\"rawMaterial\":\"TEST_Cotton\",\"weight\":50,\"unit\":\"KG\",\"ratePerUnit\":80,\"gstPercentage\":5,\"saleDate\":\"2026-09-01\"}" \
      201 "Add sale valid - CLIENT1" "$M"
    C1_SALE_ID=$(extract_public_id)
fi

# Add valid sale - ADMIN
if [ -n "$ADMIN_CUST_ID" ] && [ "$ADMIN_CUST_ID" != "null" ]; then
    call_api POST "/sale/add" "$ADMIN_TOKEN" \
      "{\"customerPublicId\":\"$ADMIN_CUST_ID\",\"rawMaterial\":\"TEST_Cotton${RUN_ID}\",\"weight\":100,\"unit\":\"KG\",\"ratePerUnit\":150,\"gstPercentage\":18,\"saleDate\":\"2026-09-01\"}" \
      201 "Add sale valid - ADMIN" "$M"
    ADMIN_SALE_ID=$(extract_public_id)
fi

# Validation
call_api POST "/sale/add" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Add sale empty body" "$M"

call_api POST "/sale/add" "$CLIENT1_TOKEN" \
  '{"rawMaterial":"TEST_M","weight":50,"unit":"KG","ratePerUnit":80,"gstPercentage":5,"saleDate":"2026-09-01"}' \
  400 "Add sale missing customerPublicId" "$M"

call_api POST "/sale/add" "$CLIENT1_TOKEN" \
  "{\"customerPublicId\":\"$FAKE_UUID\",\"rawMaterial\":\"TEST_M\",\"weight\":50,\"unit\":\"KG\",\"ratePerUnit\":80,\"gstPercentage\":5,\"saleDate\":\"2026-09-01\"}" \
  404 "Add sale non-existent customerPublicId" "$M"

# Isolation: CLIENT2's customer in CLIENT1's sale
if [ -n "$C2_CUST_ID" ] && [ "$C2_CUST_ID" != "null" ]; then
    call_api POST "/sale/add" "$CLIENT1_TOKEN" \
      "{\"customerPublicId\":\"$C2_CUST_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":50,\"unit\":\"KG\",\"ratePerUnit\":80,\"gstPercentage\":5,\"saleDate\":\"2026-09-01\"}" \
      404 "Add sale using another CLIENT's customer" "$M"
fi

if [ -n "$C1_CUST_ID" ] && [ "$C1_CUST_ID" != "null" ]; then
    call_api POST "/sale/add" "$CLIENT1_TOKEN" \
      "{\"customerPublicId\":\"$C1_CUST_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":0,\"unit\":\"KG\",\"ratePerUnit\":80,\"gstPercentage\":5,\"saleDate\":\"2026-09-01\"}" \
      400 "Add sale weight=0" "$M"

    call_api POST "/sale/add" "$CLIENT1_TOKEN" \
      "{\"customerPublicId\":\"$C1_CUST_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":50,\"unit\":\"KG\",\"ratePerUnit\":0,\"gstPercentage\":5,\"saleDate\":\"2026-09-01\"}" \
      400 "Add sale ratePerUnit=0" "$M"

    call_api POST "/sale/add" "$CLIENT1_TOKEN" \
      "{\"customerPublicId\":\"$C1_CUST_ID\",\"rawMaterial\":\"TEST_M\",\"weight\":50,\"unit\":\"KG\",\"ratePerUnit\":80,\"gstPercentage\":150,\"saleDate\":\"2026-09-01\"}" \
      400 "Add sale gstPercentage=150 (>100)" "$M"
fi

# Isolation
if [ -n "$C1_SALE_ID" ] && [ "$C1_SALE_ID" != "null" ]; then
    call_api GET "/sale/$C1_SALE_ID" "$CLIENT2_TOKEN" NONE 404 "Isolation: CLIENT2 reads CLIENT1 sale" "$M"
fi

# =============================================================================
# MODULE: SalePayment
# =============================================================================
M="SalePayment"
log_header "MODULE: $M (/sale-payment)"

call_api GET "/sale-payment/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/sale-payment/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent sale payment" "$M"

# Add valid payment (CLIENT1)
if [ -n "$C1_SALE_ID" ] && [ "$C1_SALE_ID" != "null" ]; then
    call_api POST "/sale-payment/add" "$CLIENT1_TOKEN" \
      "{\"salePublicId\":\"$C1_SALE_ID\",\"amountReceived\":200,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\",\"remarks\":\"TEST partial received\"}" \
      201 "Add sale payment valid - CLIENT1" "$M"
    C1_SP_ID=$(extract_public_id)
    # Verify paymentNumber is system-generated and present
    C1_SP_NUM=$(extract_field "paymentNumber")
    if [ -n "$C1_SP_NUM" ] && [ "$C1_SP_NUM" != "null" ]; then
        log "[PASS] [$M] paymentNumber present in response -> $C1_SP_NUM"
        MODULE_PASS[$M]=$(( ${MODULE_PASS[$M]:-0} + 1 ))
    else
        log "[FAIL] [$M] paymentNumber missing or null in creation response"
        MODULE_FAIL[$M]=$(( ${MODULE_FAIL[$M]:-0} + 1 ))
    fi
fi

# Add valid payment (ADMIN)
if [ -n "$ADMIN_SALE_ID" ] && [ "$ADMIN_SALE_ID" != "null" ]; then
    call_api POST "/sale-payment/add" "$ADMIN_TOKEN" \
      "{\"salePublicId\":\"$ADMIN_SALE_ID\",\"amountReceived\":500,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"UPI\",\"remarks\":\"TEST admin received\"}" \
      201 "Add sale payment valid - ADMIN" "$M"
fi

# Validation
call_api POST "/sale-payment/add" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Add sale payment empty body" "$M"

if [ -n "$C1_SALE_ID" ] && [ "$C1_SALE_ID" != "null" ]; then
    call_api POST "/sale-payment/add" "$CLIENT1_TOKEN" \
      "{\"salePublicId\":\"$C1_SALE_ID\",\"amountReceived\":0,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\"}" \
      400 "Add sale payment amountReceived=0" "$M"

    call_api POST "/sale-payment/add" "$CLIENT1_TOKEN" \
      "{\"salePublicId\":\"$C1_SALE_ID\",\"amountReceived\":-100,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\"}" \
      400 "Add sale payment negative amountReceived" "$M"

    call_api POST "/sale-payment/add" "$CLIENT1_TOKEN" \
      "{\"salePublicId\":\"$C1_SALE_ID\",\"amountReceived\":100,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CRYPTO\"}" \
      400 "Add sale payment invalid paymentMode enum" "$M"

    # Overpayment test
    call_api POST "/sale-payment/add" "$CLIENT1_TOKEN" \
      "{\"salePublicId\":\"$C1_SALE_ID\",\"amountReceived\":9999999,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\",\"remarks\":\"TEST overpayment\"}" \
      400 "Add sale payment overpayment" "$M"
    log "       NOTE: Sale overpayment - check if allowed (200) or rejected (400). Flag if 500."
fi

# Non-existent salePublicId
call_api POST "/sale-payment/add" "$CLIENT1_TOKEN" \
  "{\"salePublicId\":\"$FAKE_UUID\",\"amountReceived\":100,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\"}" \
  404 "Add sale payment non-existent salePublicId" "$M"

# Isolation
if [ -n "$C1_SALE_ID" ] && [ "$C1_SALE_ID" != "null" ]; then
    call_api POST "/sale-payment/add" "$CLIENT2_TOKEN" \
      "{\"salePublicId\":\"$C1_SALE_ID\",\"amountReceived\":100,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"CASH\"}" \
      404 "Isolation: CLIENT2 pays CLIENT1 sale" "$M"

    # Verify optional referenceNumber can be provided
    call_api POST "/sale-payment/add" "$CLIENT1_TOKEN" \
      "{\"salePublicId\":\"$C1_SALE_ID\",\"amountReceived\":50,\"paymentDate\":\"2026-09-01\",\"paymentMode\":\"BANK_TRANSFER\",\"referenceNumber\":\"TXN-TEST-$RANDOM\"}" \
      201 "Add sale payment with optional referenceNumber" "$M"
fi

# =============================================================================
# MODULE: Expense
# =============================================================================
M="Expense"
log_header "MODULE: $M (/expense)"

call_api GET "/expense/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/expense/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent expense" "$M"
call_api GET "/expense/abc123" "$CLIENT1_TOKEN" NONE 400 "Get malformed UUID" "$M"

# Add valid - CLIENT1 (No referenceNumber)
call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"category":"RENT","amount":5000,"expenseDate":"2026-09-01","paymentMode":"BANK_TRANSFER","description":"TEST Monthly office rent"}' \
  201 "Add expense valid - CLIENT1" "$M"
C1_EXP_ID=$(extract_public_id)

C1_EXP_NUM=$(extract_field "expenseNumber")
if [ -n "$C1_EXP_NUM" ] && [ "$C1_EXP_NUM" != "null" ]; then
    log "[PASS] [$M] expenseNumber present in response -> $C1_EXP_NUM"
    MODULE_PASS[$M]=$(( ${MODULE_PASS[$M]:-0} + 1 ))
else
    log "[FAIL] [$M] expenseNumber missing or null in creation response"
    MODULE_FAIL[$M]=$(( ${MODULE_FAIL[$M]:-0} + 1 ))
fi

# Add valid - CLIENT1 (With referenceNumber)
call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"category":"OFFICE_SUPPLIES","amount":1500,"expenseDate":"2026-09-01","paymentMode":"CHEQUE","referenceNumber":"CHQ-123456","description":"TEST Equipment with reference"}' \
  201 "Add expense with optional referenceNumber - CLIENT1" "$M"

# Add with expenseNumber in payload (should be ignored and generate a new one)
call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"category":"MAINTENANCE","amount":200,"expenseDate":"2026-09-01","paymentMode":"CASH","expenseNumber":"FAKE-999999","description":"TEST malicious expenseNumber override"}' \
  201 "Add expense malicious expenseNumber override (ignored)" "$M"
FAKE_EXP_NUM=$(extract_field "expenseNumber")
if [ "$FAKE_EXP_NUM" != "FAKE-999999" ] && [ -n "$FAKE_EXP_NUM" ] && [[ "$FAKE_EXP_NUM" == EXP-* ]]; then
    log "[PASS] [$M] Malicious expenseNumber override ignored, system generated -> $FAKE_EXP_NUM"
    MODULE_PASS[$M]=$(( ${MODULE_PASS[$M]:-0} + 1 ))
else
    log "[FAIL] [$M] Malicious expenseNumber override was accepted or generation failed -> $FAKE_EXP_NUM"
    MODULE_FAIL[$M]=$(( ${MODULE_FAIL[$M]:-0} + 1 ))
fi

# Add valid - ADMIN
call_api POST "/expense/add" "$ADMIN_TOKEN" \
  '{"category":"SALARY","amount":50000,"expenseDate":"2026-09-01","paymentMode":"BANK_TRANSFER","description":"TEST Admin salary expense"}' \
  201 "Add expense valid - ADMIN" "$M"
ADMIN_EXP_ID=$(extract_public_id)

# Validation
call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Add expense empty body" "$M"

call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"amount":1000,"expenseDate":"2026-09-01","paymentMode":"CASH","description":"TEST desc"}' \
  400 "Add expense missing category" "$M"

call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"category":"BOGUS_CATEGORY","amount":1000,"expenseDate":"2026-09-01","paymentMode":"CASH","description":"TEST desc"}' \
  400 "Add expense invalid category enum" "$M"

call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"category":"RENT","expenseDate":"2026-09-01","paymentMode":"CASH","description":"TEST desc"}' \
  400 "Add expense missing amount" "$M"

call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"category":"RENT","amount":0,"expenseDate":"2026-09-01","paymentMode":"CASH","description":"TEST desc"}' \
  400 "Add expense amount=0" "$M"

call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"category":"RENT","amount":-500,"expenseDate":"2026-09-01","paymentMode":"CASH","description":"TEST desc"}' \
  400 "Add expense negative amount" "$M"

call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"category":"OTHER","amount":1000,"expenseDate":"2026-09-01","paymentMode":"CASH"}' \
  400 "Add expense missing description for OTHER category" "$M"

call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  '{"category":"RENT","amount":1000,"expenseDate":"2026-09-01","paymentMode":"CASH"}' \
  201 "Add expense optional description for RENT category" "$M"

# Description too long (>255)
LONG_DESC=$(python3 -c "print('X'*256)")
call_api POST "/expense/add" "$CLIENT1_TOKEN" \
  "{\"category\":\"RENT\",\"amount\":1000,\"expenseDate\":\"2026-09-01\",\"paymentMode\":\"CASH\",\"description\":\"$LONG_DESC\"}" \
  400 "Add expense description too long (>255 chars)" "$M"

# Date range with fromDate > toDate
call_api GET "/expense/date-range?fromDate=2026-12-31&toDate=2026-01-01" "$CLIENT1_TOKEN" NONE \
  200 "Expense date-range reversed (graceful - empty list or 400, not 500)" "$M"
log "       NOTE: Reversed expense date range - check if 500."

# Isolation
if [ -n "$C1_EXP_ID" ] && [ "$C1_EXP_ID" != "null" ]; then
    call_api GET "/expense/$C1_EXP_ID" "$CLIENT2_TOKEN" NONE 404 "Isolation: CLIENT2 reads CLIENT1 expense" "$M"
fi

# =============================================================================
# MODULE: Partner
# =============================================================================
M="Partner"
log_header "MODULE: $M (/partner)"

call_api GET "/partner/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/partner/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent partner" "$M"
call_api GET "/partner/abc123" "$CLIENT1_TOKEN" NONE 400 "Get malformed UUID" "$M"

# Cleanup existing active partners for CLIENT1 so share percentages don't exceed 100%
ACTIVES_C1=$(curl -s -X GET "$API_URL/partner/active" -H "Authorization: Bearer $CLIENT1_TOKEN" | python3 -c "import sys, json; data=json.load(sys.stdin); print(' '.join(p.get('publicId', '') for p in data.get('data', [])))" 2>/dev/null)
for p in $ACTIVES_C1; do
    if [ -n "$p" ]; then curl -s -X PATCH "$API_URL/partner/$p/deactivate" -H "Authorization: Bearer $CLIENT1_TOKEN" > /dev/null; fi
done

# Add valid - CLIENT1
call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  "{\"partnerName\":\"TEST_Partner One\",\"mobileNumber\":\"930${RUN_ID}01\",\"email\":\"test.partner1${RUN_ID}@example.com\",\"sharePercentage\":30,\"joiningDate\":\"2026-01-01\"}" \
  200 "Add partner valid (30%) - CLIENT1" "$M"
C1_PARTNER1_ID=$(extract_public_id)

call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  "{\"partnerName\":\"TEST_Partner Two\",\"mobileNumber\":\"930${RUN_ID}02\",\"email\":\"test.partner2${RUN_ID}@example.com\",\"sharePercentage\":40,\"joiningDate\":\"2026-01-01\"}" \
  200 "Add partner valid (40%) - CLIENT1" "$M"
C1_PARTNER2_ID=$(extract_public_id)

call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  "{\"partnerName\":\"TEST_Partner Three\",\"mobileNumber\":\"930${RUN_ID}03\",\"email\":\"test.partner3${RUN_ID}@example.com\",\"sharePercentage\":30,\"joiningDate\":\"2026-01-01\"}" \
  200 "Add partner valid (30% to reach 100%) - CLIENT1" "$M"

# Cleanup existing active partners for ADMIN
ACTIVES_ADMIN=$(curl -s -X GET "$API_URL/partner/active" -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -c "import sys, json; data=json.load(sys.stdin); print(' '.join(p.get('publicId', '') for p in data.get('data', [])))" 2>/dev/null)
for p in $ACTIVES_ADMIN; do
    if [ -n "$p" ]; then curl -s -X PATCH "$API_URL/partner/$p/deactivate" -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null; fi
done

# Add valid - ADMIN
call_api POST "/partner/add" "$ADMIN_TOKEN" \
  "{\"partnerName\":\"TEST_AdminPartner\",\"mobileNumber\":\"930${RUN_ID}10\",\"email\":\"test.adminpartner${RUN_ID}@example.com\",\"sharePercentage\":50,\"joiningDate\":\"2026-01-01\"}" \
  200 "Add partner valid (50%) - ADMIN" "$M"
ADMIN_PARTNER_ID=$(extract_public_id)

call_api POST "/partner/add" "$ADMIN_TOKEN" \
  "{\"partnerName\":\"TEST_AdminPartner Two\",\"mobileNumber\":\"930${RUN_ID}11\",\"email\":\"test.adminpartner2${RUN_ID}@example.com\",\"sharePercentage\":50,\"joiningDate\":\"2026-01-01\"}" \
  200 "Add partner valid (50% to reach 100%) - ADMIN" "$M"

# Validation
call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Add partner empty body" "$M"

call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  '{"mobileNumber":"9300000099","sharePercentage":10,"joiningDate":"2026-01-01"}' \
  400 "Add partner missing partnerName" "$M"

call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  '{"partnerName":"TEST_P","sharePercentage":10,"joiningDate":"2026-01-01"}' \
  400 "Add partner missing mobileNumber" "$M"

call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  '{"partnerName":"TEST_P","mobileNumber":"9300000099","joiningDate":"2026-01-01"}' \
  400 "Add partner missing sharePercentage" "$M"

call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  '{"partnerName":"TEST_P","mobileNumber":"9300000099","sharePercentage":0,"joiningDate":"2026-01-01"}' \
  400 "Add partner sharePercentage=0 (<= 0)" "$M"

call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  '{"partnerName":"TEST_P","mobileNumber":"9300000099","sharePercentage":101,"joiningDate":"2026-01-01"}' \
  400 "Add partner sharePercentage=101 (> 100)" "$M"

call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  '{"partnerName":"TEST_P","mobileNumber":"9300000099","sharePercentage":-1,"joiningDate":"2026-01-01"}' \
  400 "Add partner negative sharePercentage" "$M"

# Total share exceeds 100% - CLIENT1 already has 30+40=70%, adding 40% more should exceed
call_api POST "/partner/add" "$CLIENT1_TOKEN" \
  '{"partnerName":"TEST_PartnerExcess","mobileNumber":"9300000098","email":"test.excess@example.com","sharePercentage":40,"joiningDate":"2026-01-01"}' \
  400 "Add partner total share exceeds 100% (70+40=110)" "$M"
log "       NOTE: Total partner share check - expected 400. Flag if 500 or if allowed."

# Isolation
if [ -n "$C1_PARTNER1_ID" ] && [ "$C1_PARTNER1_ID" != "null" ]; then
    call_api GET "/partner/$C1_PARTNER1_ID" "$CLIENT2_TOKEN" NONE 404 "Isolation: CLIENT2 reads CLIENT1 partner" "$M"
fi

# Deactivate/reactivate
if [ -n "$C1_PARTNER1_ID" ] && [ "$C1_PARTNER1_ID" != "null" ]; then
    call_api PATCH "/partner/$C1_PARTNER1_ID/deactivate" "$CLIENT1_TOKEN" NONE 200 "Deactivate partner - CLIENT1" "$M"
    call_api PATCH "/partner/$C1_PARTNER1_ID/deactivate" "$CLIENT1_TOKEN" NONE 200 "Deactivate already-inactive partner (idempotent)" "$M"
    call_api PATCH "/partner/$C1_PARTNER1_ID/reactivate" "$CLIENT1_TOKEN" NONE 200 "Reactivate partner - CLIENT1" "$M"
fi

# =============================================================================
# MODULE: ProfitDistribution
# =============================================================================
M="ProfitDistribution"
log_header "MODULE: $M (/profit-distribution)"

call_api GET "/profit-distribution/all" NONE NONE 401 "Get all - no auth" "$M"
call_api GET "/profit-distribution/$FAKE_UUID" "$CLIENT1_TOKEN" NONE 404 "Get non-existent distribution" "$M"

# Valid distribution (CLIENT1 has partners; may have zero data but should still succeed)
call_api POST "/profit-distribution/distribute" "$CLIENT1_TOKEN" \
  "{\"fromDate\":\"${PD_YEAR}-09-01\",\"toDate\":\"${PD_YEAR}-09-30\"}" \
  200 "Distribute profits valid (CLIENT1) - zeroed result if no data" "$M"
log "       NOTE: Distribution with zero data - should return zeroed result not exception."
C1_DIST_ID=$(extract_public_id)

# Valid distribution - ADMIN
call_api POST "/profit-distribution/distribute" "$ADMIN_TOKEN" \
  "{\"fromDate\":\"${PD_YEAR}-09-01\",\"toDate\":\"${PD_YEAR}-09-30\"}" \
  200 "Distribute profits valid (ADMIN)" "$M"

# Duplicate range (run same range again for CLIENT1)
call_api POST "/profit-distribution/distribute" "$CLIENT1_TOKEN" \
  "{\"fromDate\":\"${PD_YEAR}-09-01\",\"toDate\":\"${PD_YEAR}-09-30\"}" \
  400 "Distribute duplicate fromDate/toDate range (same user)" "$M"
log "       NOTE: Duplicate range - expected 400 clean error. Flag if 500."

# fromDate after toDate
call_api POST "/profit-distribution/distribute" "$CLIENT1_TOKEN" \
  '{"fromDate":"2026-12-31","toDate":"2026-01-01"}' \
  400 "Distribute fromDate after toDate" "$M"
log "       NOTE: Inverted date range - expected 400. Flag if 500 or if it succeeds."

# Missing required fields
call_api POST "/profit-distribution/distribute" "$CLIENT1_TOKEN" \
  '{}' \
  400 "Distribute missing fromDate and toDate" "$M"

call_api POST "/profit-distribution/distribute" "$CLIENT1_TOKEN" \
  '{"fromDate":"2026-08-01"}' \
  400 "Distribute missing toDate" "$M"

# Isolation: CLIENT2 reads CLIENT1's distribution
if [ -n "$C1_DIST_ID" ] && [ "$C1_DIST_ID" != "null" ]; then
    call_api GET "/profit-distribution/$C1_DIST_ID" "$CLIENT2_TOKEN" NONE 404 "Isolation: CLIENT2 reads CLIENT1 distribution" "$M"
fi

# =============================================================================
# MODULE: Dashboard
# =============================================================================
M="Dashboard"
log_header "MODULE: $M (/dashboard)"

call_api GET "/dashboard" NONE NONE 401 "Dashboard - no auth" "$M"
call_api GET "/dashboard" "$CLIENT1_TOKEN" NONE 200 "Dashboard valid - CLIENT1" "$M"
call_api GET "/dashboard" "$ADMIN_TOKEN" NONE 200 "Dashboard valid - ADMIN" "$M"

# =============================================================================
# MODULE: Report
# =============================================================================
M="Report"
log_header "MODULE: $M (/report)"

call_api GET "/report/sales?from=2026-01-01&to=2026-09-30" NONE NONE 401 "Sales report - no auth" "$M"
call_api GET "/report/sales?from=2026-01-01&to=2026-09-30" "$CLIENT1_TOKEN" NONE 200 "Sales report valid - CLIENT1" "$M"
call_api GET "/report/sales?from=2026-01-01&to=2026-09-30" "$ADMIN_TOKEN" NONE 200 "Sales report valid - ADMIN" "$M"

# Reversed date range
call_api GET "/report/sales?from=2026-12-31&to=2026-01-01" "$CLIENT1_TOKEN" NONE 200 "Sales report reversed dates (empty or 400)" "$M"
log "       NOTE: Sales report reversed dates - should be empty list or 400, not 500."

# Missing required params
call_api GET "/report/sales" "$CLIENT1_TOKEN" NONE 400 "Sales report missing from/to params" "$M"
call_api GET "/report/sales?from=2026-01-01" "$CLIENT1_TOKEN" NONE 400 "Sales report missing to param" "$M"

call_api GET "/report/purchases?from=2026-01-01&to=2026-09-30" "$CLIENT1_TOKEN" NONE 200 "Purchases report valid - CLIENT1" "$M"
call_api GET "/report/purchases?from=2026-12-31&to=2026-01-01" "$CLIENT1_TOKEN" NONE 200 "Purchases report reversed dates" "$M"
log "       NOTE: Purchase report reversed dates - check if 500."
call_api GET "/report/purchases" "$CLIENT1_TOKEN" NONE 400 "Purchases report missing params" "$M"

call_api GET "/report/expenses?from=2026-01-01&to=2026-09-30" "$CLIENT1_TOKEN" NONE 200 "Expenses report valid - CLIENT1" "$M"
call_api GET "/report/expenses?from=2026-12-31&to=2026-01-01" "$CLIENT1_TOKEN" NONE 200 "Expenses report reversed dates" "$M"
log "       NOTE: Expense report reversed dates - check if 500."
call_api GET "/report/expenses" "$CLIENT1_TOKEN" NONE 400 "Expenses report missing params" "$M"

call_api GET "/report/profit-loss?from=2026-01-01&to=2026-09-30" "$CLIENT1_TOKEN" NONE 200 "P&L report valid - CLIENT1" "$M"
call_api GET "/report/profit-loss?from=2026-12-31&to=2026-01-01" "$CLIENT1_TOKEN" NONE 200 "P&L report reversed dates" "$M"
log "       NOTE: P&L report reversed dates - check if 500."
call_api GET "/report/profit-loss" "$CLIENT1_TOKEN" NONE 400 "P&L report missing params" "$M"

call_api GET "/report/stock" "$CLIENT1_TOKEN" NONE 200 "Stock report valid - CLIENT1" "$M"
call_api GET "/report/stock" "$ADMIN_TOKEN" NONE 200 "Stock report valid - ADMIN" "$M"
call_api GET "/report/stock" NONE NONE 401 "Stock report - no auth" "$M"

call_api GET "/report/customers/outstanding" "$CLIENT1_TOKEN" NONE 200 "Customer outstanding report - CLIENT1" "$M"
call_api GET "/report/suppliers/outstanding" "$CLIENT1_TOKEN" NONE 200 "Supplier outstanding report - CLIENT1" "$M"

# =============================================================================
# FINAL SUMMARY
# =============================================================================

MODULES=("Auth" "Admin" "Customer" "Supplier" "Stock" "StockTransaction" "Purchase" "PurchasePayment" "Sale" "SalePayment" "Expense" "Partner" "ProfitDistribution" "Dashboard" "Report")

print_summary "${MODULES[@]}"

log ""
log "Full log saved to: $LOG_FILE"
log ""
log "======================================================================"
log " POST-RUN NOTES"
log "======================================================================"
log " - All test data prefixed with 'TEST_' for easy identification in UI."
log " - testclient1@example.com and testclient2@example.com may have new"
log "   customers/suppliers/stocks/expenses/partners created."
log " - Check for [FAIL] lines with '*** RAW 500 ***' for priority bugs."
log " - Check lines with 'NOTE:' for ambiguous behavior requiring decisions."
log "======================================================================"
