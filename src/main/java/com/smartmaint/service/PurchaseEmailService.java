package com.smartmaint.service;

import com.smartmaint.model.PlanEmpresa;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PurchaseEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;
    private final String smtpUsername;
    private final String loginUrl;

    public PurchaseEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.purchase.mail.from:no-reply@smartmaint.com}") String from,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${app.purchase.login.url:http://localhost:3000/login}") String loginUrl
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
        this.smtpUsername = smtpUsername;
        this.loginUrl = loginUrl;
    }

    public void enviarResumenCompraYCredenciales(Set<String> destinatarios,
                                                  String nombreCompleto,
                                                  String idInstitucional,
                                                  PlanEmpresa plan,
                                                  String correoAcceso,
                                                  String contrasenaTemporal) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            throw new IllegalArgumentException("No hay destinatarios para enviar credenciales.");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("El envío de correo no está configurado. Define SPRING_MAIL_HOST y credenciales SMTP.");
        }

        String asunto = "Compra confirmada y credenciales de acceso - SmartMaint";
        String planTexto = plan == PlanEmpresa.ANUAL ? "Plan anual" : "Plan mensual";

        String textoPlano = "Hola " + nombreCompleto + ",\n\n"
                + "Tu compra fue confirmada.\n"
                + "Detalle del plan: " + planTexto + "\n"
                + "ID institucional: " + idInstitucional + "\n\n"
                + "Credenciales de acceso:\n"
                + "Correo: " + correoAcceso + "\n"
                + "Contraseña temporal: " + contrasenaTemporal + "\n\n"
                + "Al ingresar por primera vez, se te solicitará cambiar esta contraseña.\n"
                + "Iniciar sesión: " + loginUrl + "\n\n"
                + "Equipo SmartMaint";

        String html = """
                <html lang=\"es\">
                <body style=\"font-family:Segoe UI,Arial,sans-serif;background:#f3f7ff;padding:16px;\">
                  <div style=\"max-width:680px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #d7e4f8;\">
                    <div style=\"background:linear-gradient(90deg,#0b3f83,#0a4a95 55%,#0f5db8);padding:16px 20px;color:#ffffff;font-weight:800;font-size:22px;\">SMARTMAINT</div>
                    <div style=\"padding:20px;\">
                      <p style=\"margin:0 0 12px;\">Hola <strong>__NOMBRE__</strong>,</p>
                      <p style=\"margin:0 0 12px;line-height:1.6;color:#334155;\">Tu compra fue confirmada y tu cuenta de <strong>SuperAdmin</strong> ya está creada.</p>
                      <div style=\"background:#f8fbff;border:1px solid #d8e6fb;border-radius:10px;padding:14px;margin-bottom:14px;\">
                        <div style=\"font-weight:800;color:#0b3f83;margin-bottom:8px;\">Detalle de compra</div>
                        <div><strong>Plan:</strong> __PLAN__</div>
                        <div><strong>ID institucional:</strong> __ID_INSTITUCIONAL__</div>
                      </div>
                      <div style=\"background:#fff8e6;border:1px solid #f7d778;border-radius:10px;padding:14px;margin-bottom:14px;\">
                        <div style=\"font-weight:800;color:#0b3f83;margin-bottom:8px;\">Credenciales de acceso</div>
                        <div><strong>Correo:</strong> __CORREO__</div>
                        <div><strong>Contraseña temporal:</strong> __PASS__</div>
                      </div>
                      <p style=\"margin:0 0 12px;line-height:1.6;color:#334155;\">En el primer inicio de sesión, el sistema te pedirá cambiar la contraseña temporal por una nueva.</p>
                      <a href=\"__LOGIN_URL__\" style=\"display:inline-block;background:#0b4a95;color:#fff;text-decoration:none;padding:10px 14px;border-radius:8px;font-weight:700;\">Ir a iniciar sesión</a>
                    </div>
                  </div>
                </body>
                </html>
                """
                .replace("__NOMBRE__", escaparHtml(nombreCompleto))
                .replace("__PLAN__", escaparHtml(planTexto))
                .replace("__ID_INSTITUCIONAL__", escaparHtml(idInstitucional))
                .replace("__CORREO__", escaparHtml(correoAcceso))
                .replace("__PASS__", escaparHtml(contrasenaTemporal))
                .replace("__LOGIN_URL__", escaparHtml(loginUrl));

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(new InternetAddress(resolverRemitente(), "SmartMaint Notificaciones", "UTF-8"));
            helper.setReplyTo("no-reply@smartmaint.com");
            helper.setTo(destinatarios.toArray(new String[0]));
            helper.setSubject(asunto);
            helper.setText(textoPlano, html);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo de compra y credenciales", e);
        }
    }

    private String resolverRemitente() {
        if (from != null && !from.isBlank()) {
            return from;
        }
        if (smtpUsername != null && !smtpUsername.isBlank()) {
            return smtpUsername;
        }
        throw new IllegalStateException("No hay remitente configurado para correo de compra.");
    }

    private String escaparHtml(String valor) {
        if (valor == null) return "";
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
