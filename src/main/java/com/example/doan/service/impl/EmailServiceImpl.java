package com.example.doan.service.impl;

import com.example.doan.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public void sendOtp(String toEmail, String otpCode) {
        log.info("==================================================");
        log.info("MÃ OTP ĐĂNG NHẬP GỬI ĐẾN {}: {}", toEmail, otpCode);
        log.info("==================================================");
        
        if (mailSender == null) {
            log.warn("JavaMailSender chưa được cấu hình. Chỉ ghi nhận OTP ở log console.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Mã xác thực OTP đăng nhập NEXUS Tech");
            message.setText("Chào bạn, mã OTP đăng nhập vào hệ thống NEXUS Tech của bạn là: " + otpCode 
                    + "\nMã OTP có giá trị trong vòng 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.");
            mailSender.send(message);
            log.info("Đã gửi email OTP thực tế đến: {}", toEmail);
        } catch (Exception e) {
            log.warn("Không thể gửi email OTP thực tế (vui lòng kiểm tra lại SMTP). Chi tiết lỗi: {}", e.getMessage());
        }
    }
}
