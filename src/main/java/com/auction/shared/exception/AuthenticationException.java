package com.auction.shared.exception;

public class AuthenticationException extends Exception {

    public enum Reason {
        WRONG_PASSWORD,   // Mật khẩu không khớp
        USER_NOT_FOUND,   // Không tìm thấy username trong DB
        ACCOUNT_INACTIVE  // Tài khoản bị Admin khóa (isActive = false)
    }

    private final Reason reason; // Lý do cụ thể

    public AuthenticationException(Reason reason) {
        // Tự tạo message tiếng Việt dựa trên reason
        super(buildMessage(reason));
        this.reason = reason;
    }

    private static String buildMessage(Reason reason) {
        // Switch expression (Java 14+) — gọn hơn switch statement
        return switch (reason) {
            case WRONG_PASSWORD   -> "Mật khẩu không đúng. Vui lòng thử lại.";
            case USER_NOT_FOUND   -> "Tài khoản không tồn tại.";
            case ACCOUNT_INACTIVE -> "Tài khoản đã bị khóa. Vui lòng liên hệ Admin.";
        };
    }

    public Reason getReason() { return reason; }
}
