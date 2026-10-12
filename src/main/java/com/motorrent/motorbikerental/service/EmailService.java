package com.motorrent.motorbikerental.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("Chào mừng đến với MotoRent Đà Nẵng!");
            
            String htmlContent = "<h2>Xin chào " + name + ",</h2>"
                    + "<p>Cảm ơn bạn đã đăng ký tài khoản tại MotoRent Đà Nẵng.</p>"
                    + "<p>Chúng tôi cung cấp dịch vụ cho thuê xe máy chất lượng cao với giá cả hợp lý nhất.</p>"
                    + "<p>Chúc bạn có một trải nghiệm tuyệt vời cùng MotoRent!</p>";
                    
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("Mã OTP khôi phục mật khẩu - MotoRent Đà Nẵng");
            
            String htmlContent = "<h2>Khôi phục mật khẩu</h2>"
                    + "<p>Bạn đã yêu cầu khôi phục mật khẩu. Dưới đây là mã OTP của bạn:</p>"
                    + "<h1 style='color: #f97316; font-size: 32px; letter-spacing: 5px;'>" + otpCode + "</h1>"
                    + "<p>Mã này sẽ hết hạn trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>";
                    
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
