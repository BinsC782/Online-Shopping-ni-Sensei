# Changelog

## [2025-10-06] - Code Reorganization and Cleanup

### Added
- Standard Maven-like directory structure for better code organization
- New package structure:
  - `com.shopping.handlers` - HTTP request handlers
  - `com.shopping.service` - Business logic services
  - `com.shopping.util` - Utility classes
  - `com.shopping.config` - Configuration classes
  - `com.shopping.exception` - Custom exceptions
- Documentation for the new project structure

### Changed
- Moved HTTP handlers to `com.shopping.handlers` package
- Moved business logic to `com.shopping.service` package
- Updated import statements to reflect new package structure
- Cleaned up unused imports and fixed compilation errors

### Fixed
- Resource leaks in file handling
- Syntax errors in various files
- Type safety warnings
- CORS handling in the server

### Removed
- Unused imports
- Redundant code
- Placeholder comments

## [Previous Changes]

### 2025-09-18 - OnlineShopping Beta V0.1
- Initial commit of the online shopping application
- Basic file-based data persistence
- Console-based user interface
- Product browsing and cart functionality
