#!/bin/bash

# Email Service Integration Test Runner
# This script runs the Postman collection tests using Newman CLI

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COLLECTION_FILE="Email_Service_Postman_Collection.json"
ENVIRONMENT_FILE="Postman_Test_Environment.json"
RESULTS_FILE="test-results.json"
REPORT_FILE="test-report.html"

echo -e "${BLUE}Email Service Integration Test Runner${NC}"
echo "=============================================="

# Check if Newman is installed
if ! command -v newman &> /dev/null; then
    echo -e "${RED}Newman CLI is not installed. Please install it first:${NC}"
    echo "npm install -g newman"
    echo "npm install -g newman-reporter-html"
    exit 1
fi

# Check if collection file exists
if [ ! -f "$COLLECTION_FILE" ]; then
    echo -e "${RED}Collection file not found: $COLLECTION_FILE${NC}"
    exit 1
fi

# Check if environment file exists
if [ ! -f "$ENVIRONMENT_FILE" ]; then
    echo -e "${RED}Environment file not found: $ENVIRONMENT_FILE${NC}"
    exit 1
fi

# Check if service is running
echo -e "${YELLOW}Checking if Email Service is running...${NC}"
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo -e "${RED}Email Service is not running on localhost:8080${NC}"
    echo "Please start the service first:"
    echo "mvn spring-boot:run"
    exit 1
fi

echo -e "${GREEN}Email Service is running!${NC}"

# Run the tests
echo -e "${YELLOW}Running integration tests...${NC}"
echo "Collection: $COLLECTION_FILE"
echo "Environment: $ENVIRONMENT_FILE"
echo ""

# Run Newman with multiple reporters
newman run "$COLLECTION_FILE" \
    -e "$ENVIRONMENT_FILE" \
    --reporters cli,json,html \
    --reporter-json-export "$RESULTS_FILE" \
    --reporter-html-export "$REPORT_FILE" \
    --delay-request 1000 \
    --timeout-request 30000

# Check exit code
if [ $? -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    echo ""
    echo "Results saved to:"
    echo "  - JSON: $RESULTS_FILE"
    echo "  - HTML: $REPORT_FILE"
else
    echo -e "${RED}Some tests failed!${NC}"
    echo ""
    echo "Check the results:"
    echo "  - JSON: $RESULTS_FILE"
    echo "  - HTML: $REPORT_FILE"
    exit 1
fi

# Display summary
echo ""
echo -e "${BLUE}Test Summary:${NC}"
if [ -f "$RESULTS_FILE" ]; then
    # Extract test statistics using jq if available
    if command -v jq &> /dev/null; then
        TOTAL=$(jq '.run.stats.tests.total' "$RESULTS_FILE")
        PASSED=$(jq '.run.stats.tests.passed' "$RESULTS_FILE")
        FAILED=$(jq '.run.stats.tests.failed' "$RESULTS_FILE")
        
        echo "Total Tests: $TOTAL"
        echo "Passed: $PASSED"
        echo "Failed: $FAILED"
        
        if [ "$FAILED" -gt 0 ]; then
            echo -e "${RED}Failed Tests:${NC}"
            jq -r '.run.failures[] | "  - \(.error.name): \(.error.test)"' "$RESULTS_FILE"
        fi
    else
        echo "Install jq for detailed statistics: apt-get install jq"
    fi
fi

echo ""
echo -e "${BLUE}Integration tests completed!${NC}"


