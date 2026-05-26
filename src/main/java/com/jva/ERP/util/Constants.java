package com.jva.ERP.util;

/**
 * Application Constants
 * Centralized constants for the entire application
 */
public class Constants {

    private Constants() {
        // Private constructor to prevent instantiation
    }

    // ============= Authorization =============
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USER_ID_HEADER = "X-User-Id";

    // ============= API Response Codes =============
    public static final int SUCCESS_CODE = 200;
    public static final int CREATED_CODE = 201;
    public static final int BAD_REQUEST_CODE = 400;
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int FORBIDDEN_CODE = 403;
    public static final int NOT_FOUND_CODE = 404;
    public static final int CONFLICT_CODE = 409;
    public static final int ERROR_CODE = 500;

    // ============= API Response Messages =============
    public static final String SUCCESS_MESSAGE = "Operation completed successfully";
    public static final String CREATED_MESSAGE = "Resource created successfully";
    public static final String BAD_REQUEST_MESSAGE = "Invalid request";
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized access";
    public static final String FORBIDDEN_MESSAGE = "Access forbidden";
    public static final String NOT_FOUND_MESSAGE = "Resource not found";
    public static final String CONFLICT_MESSAGE = "Resource conflict";
    public static final String ERROR_MESSAGE = "An error occurred";

    // ============= Pagination =============
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // ============= Date Format =============
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_FORMAT = "HH:mm:ss";

    // ============= Validation Messages =============
    public static final String REQUIRED_FIELD = "This field is required";
    public static final String INVALID_EMAIL = "Invalid email format";
    public static final String INVALID_PHONE = "Invalid phone number";
    public static final String PASSWORD_TOO_SHORT = "Password must be at least 8 characters";
    public static final String USERNAME_ALREADY_EXISTS = "Username already exists";
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists";

    // ============= User Roles =============
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    // ============= Cache Keys =============
    public static final String CACHE_USER = "user";
    public static final String CACHE_ROLE = "role";
    public static final String CACHE_PERMISSION = "permission";

    // ============= Regex Patterns =============
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
    public static final String PHONE_REGEX = "^[+]?[0-9]{10,13}$";
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9_-]{3,16}$";

    // ============= Request/Response =============
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CHARSET_UTF8 = "UTF-8";
}

