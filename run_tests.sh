#!/bin/bash

# Minecraft Playground - Game Test Runner
# This script runs NeoForge game tests for the mod

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored messages
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Print header
echo ""
echo "========================================="
echo "  Redstone Wire - Game Test Runner"
echo "========================================="
echo ""

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    print_error "gradlew not found! Make sure you're in the project root directory."
    exit 1
fi

# Make gradlew executable if it isn't already
chmod +x ./gradlew

# Parse command line arguments
TEST_MODE=${1:-"all"}

case "$TEST_MODE" in
    "all"|"")
        print_info "Running all game tests..."
        ./gradlew runGameTestServer \
          | grep --color --regexp='ERROR.*' --regexp='^'
#          | grep --color --regexp='========= .* GAME TESTS COMPLETE IN .* ======================' --regexp='^' \
#          | grep --color --regexp='====*' --regexp='^'
        ;;
    "clean")
        print_info "Cleaning and running all game tests..."
        ./gradlew clean runGameTestServer
        ;;
    "build")
        print_info "Building mod and running game tests..."
        ./gradlew build runGameTestServer
        ;;
    "help"|"-h"|"--help")
        echo "Usage: ./run_tests.sh [MODE]"
        echo ""
        echo "Modes:"
        echo "  all      - Run all game tests (default)"
        echo "  clean    - Clean build and run tests"
        echo "  build    - Build mod and run tests"
        echo "  help     - Show this help message"
        echo ""
        echo "Examples:"
        echo "  ./run_tests.sh           # Run all tests"
        echo "  ./run_tests.sh clean     # Clean and run tests"
        echo "  ./run_tests.sh build     # Build and run tests"
        exit 0
        ;;
    *)
        print_error "Unknown mode: $TEST_MODE"
        echo "Run './run_tests.sh help' for usage information."
        exit 1
        ;;
esac

# Check exit code
if [ $? -eq 0 ]; then
    print_success "Game tests completed successfully!"
    exit 0
else
    print_error "Game tests failed! Check the output above for details."
    exit 1
fi
