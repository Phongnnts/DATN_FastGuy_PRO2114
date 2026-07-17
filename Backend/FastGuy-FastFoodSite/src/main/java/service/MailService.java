package service;

import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import utils.AppConfig;

public class MailService {
    public void sendPasswordReset(String recipient, String token) {
        if (!AppConfig.isSmtpConfigured()) throw new IllegalStateException("Email đặt lại mật khẩu chưa được cấu hình");
        Properties properties = new Properties();
        properties.put("mail.smtp.host", AppConfig.getSmtpHost());
        properties.put("mail.smtp.port", AppConfig.getSmtpPort());
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(AppConfig.getSmtpUser(), AppConfig.getSmtpPassword());
            }
        });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(AppConfig.getSmtpFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject("Đặt lại mật khẩu FastGuy", "UTF-8");
            String resetUrl = AppConfig.getAppWebUrl() + "/reset-password?token=" + token;
            message.setText("Bạn đã yêu cầu đặt lại mật khẩu FastGuy. Liên kết có hiệu lực trong 15 phút:\n" + resetUrl + "\n\nNếu không phải bạn yêu cầu, hãy bỏ qua email này.", "UTF-8");
            Transport.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể gửi email đặt lại mật khẩu", e);
        }
    }
}
