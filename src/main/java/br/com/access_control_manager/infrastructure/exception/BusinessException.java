package br.com.access_control_manager.infrastructure.exception;

public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(message, "BUSINESS_ERROR");
    }
}