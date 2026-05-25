package com.smartmaint.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DemoEmailService {

    private static final Logger log = LoggerFactory.getLogger(DemoEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;
    private final String smtpUsername;
    private final String loginUrl;
    private final String logoUrl;

    public DemoEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.demo.mail.from:no-reply@smartmaint.com}") String from,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${app.demo.login.url:http://localhost:3000/login}") String loginUrl,
            @Value("${app.demo.mail.logo.url:}") String logoUrl
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
        this.smtpUsername = smtpUsername;
        this.loginUrl = loginUrl;
        this.logoUrl = logoUrl;
    }

    public void enviarCredencialesDemo(
            String destinatario,
            String nombre,
            String empresa,
            String superAdminCorreo,
            String superAdminContrasena,
            String adminCorreo,
            String adminContrasena,
            String usuarioCorreo,
            String usuarioContrasena,
            LocalDateTime expiraEn
    ) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("El envío de correo demo no está configurado. Define SPRING_MAIL_HOST y credenciales SMTP.");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String vigenciaFormateada = expiraEn.format(formatter);

                String textoPlano =
                "Hola " + nombre + ",\n\n" +
                "Tu solicitud demo para " + empresa + " fue aprobada.\n" +
                    "Estas son tus credenciales temporales para probar los perfiles:\n\n" +
                    "SUPERADMIN\n" +
                    "Correo: " + superAdminCorreo + "\n" +
                    "Contrasena: " + superAdminContrasena + "\n\n" +
                "ADMIN\n" +
                "Correo: " + adminCorreo + "\n" +
                "Contrasena: " + adminContrasena + "\n\n" +
                "USUARIO\n" +
                "Correo: " + usuarioCorreo + "\n" +
                "Contrasena: " + usuarioContrasena + "\n\n" +
                "Vigencia: hasta " + vigenciaFormateada + "\n" +
                "(1 semana; luego se deshabilitan automaticamente)\n\n" +
                "Iniciar sesion: " + loginUrl + "\n\n" +
                "Equipo SmartMaint";

                String html = construirPlantillaHtml(
                                nombre,
                                empresa,
                    superAdminCorreo,
                    superAdminContrasena,
                                adminCorreo,
                                adminContrasena,
                                usuarioCorreo,
                                usuarioContrasena,
                                vigenciaFormateada
                );

                try {
                        MimeMessage mimeMessage = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                        helper.setFrom(new InternetAddress(resolverRemitente(), "SmartMaint Notificaciones", "UTF-8"));
                        helper.setReplyTo("no-reply@smartmaint.com");
                        helper.setTo(destinatario);
                        helper.setSubject("Tus credenciales de acceso demo - SmartMaint");
                        helper.setText(textoPlano, html);
                        log.info("Enviando correo demo a {} desde {}", destinatario, resolverRemitente());
                        mailSender.send(mimeMessage);
                        log.info("Correo demo enviado correctamente a {}", destinatario);
                } catch (MessagingException | UnsupportedEncodingException e) {
                    throw new IllegalStateException("No se pudo construir el correo de credenciales demo", e);
                } catch (Exception e) {
                    log.error("Error al enviar correo demo a {}: {}", destinatario, e.getMessage(), e);
                    throw new IllegalStateException("No se pudo enviar el correo de credenciales demo", e);
                }
    }

    private String resolverRemitente() {
        // Prioriza el remitente de negocio configurable (app.demo.mail.from).
        // Si no está definido, usa el usuario SMTP como fallback.
        if (from != null && !from.isBlank()) {
            return from;
        }
        if (smtpUsername != null && !smtpUsername.isBlank()) {
            return smtpUsername;
        }
        throw new IllegalStateException("No hay remitente configurado para correo demo. Define spring.mail.username o app.demo.mail.from.");
    }

        private String construirPlantillaHtml(
                        String nombre,
                        String empresa,
                String superAdminCorreo,
                String superAdminContrasena,
                        String adminCorreo,
                        String adminContrasena,
                        String usuarioCorreo,
                        String usuarioContrasena,
                        String vigenciaFormateada
        ) {
                String nombreSeguro = escaparHtml(nombre);
                String empresaSegura = escaparHtml(empresa);
            String superAdminCorreoSeguro = escaparHtml(superAdminCorreo);
            String superAdminContrasenaSegura = escaparHtml(superAdminContrasena);
                String adminCorreoSeguro = escaparHtml(adminCorreo);
                String adminContrasenaSegura = escaparHtml(adminContrasena);
                String usuarioCorreoSeguro = escaparHtml(usuarioCorreo);
                String usuarioContrasenaSegura = escaparHtml(usuarioContrasena);
                String vigenciaSegura = escaparHtml(vigenciaFormateada);

        String logoUrlSegura = escaparHtml(logoUrl == null ? "" : logoUrl);
        String loginUrlSegura = escaparHtml(loginUrl == null ? "" : loginUrl);
        String bloqueLogo = (logoUrl != null && !logoUrl.isBlank())
                ? "<img src=\"__LOGO_URL__\" alt=\"SMARTMAINT\" style=\"max-height:36px;display:block;\"/>"
                : "<span style=\"font-size:24px;font-weight:800;letter-spacing:0.02em;color:#ffffff;\">SMARTMAINT</span>";

        String plantilla = """
                                <!doctype html>
                                <html lang=\"es\">
                                <head>
                                    <meta charset=\"UTF-8\" />
                                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
                                    <title>Credenciales Demo SmartMaint</title>
                                </head>
                                <body style=\"margin:0;padding:0;background:#f3f7ff;font-family:'Segoe UI',Arial,sans-serif;color:#1f2a37;\">
                                    <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px 10px;\">
                                        <tr>
                                            <td align=\"center\">
                                                <table role=\"presentation\" width=\"680\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:680px;width:100%;background:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 10px 32px rgba(11,63,131,0.14);\">
                                                    <tr>
                                                        <td style=\"background:linear-gradient(90deg,#0b3f83,#0a4a95 55%,#0f5db8);padding:18px 24px;color:#ffffff;font-size:24px;font-weight:800;letter-spacing:0.02em;\">
                                                            SMARTMAINT
                                                        </td>
                                                    </tr>
                                                        <td style="background:linear-gradient(90deg,#0b3f83,#0a4a95 55%,#0f5db8);padding:18px 24px;">
                                                            __LOGO_BLOCK__
                                                            <p style=\"margin:0 0 12px;font-size:16px;line-height:1.5;\">Hola <strong>__NOMBRE__</strong>,</p>
                                                            <p style=\"margin:0 0 16px;font-size:15px;line-height:1.6;color:#334155;\">
                                                                Tu solicitud demo para <strong>__EMPRESA__</strong> fue aprobada.
                                                                A continuación encontrarás tus credenciales temporales para probar ambos paneles.
                                                            </p>

                                                            <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:separate;border-spacing:0 12px;\">
                                                                                                        <tr>
                                                                                                            <td style=\"background:#f8fbff;border:1px solid #d8e6fb;border-radius:10px;padding:14px 16px;\">
                                                                                                                <div style=\"font-size:14px;font-weight:800;color:#0b3f83;margin-bottom:8px;\">SUPERADMIN</div>
                                                                                                                <div style=\"font-size:14px;line-height:1.65;\"><strong>Correo:</strong> __SUPERADMIN_CORREO__</div>
                                                                                                                <div style=\"font-size:14px;line-height:1.65;\"><strong>Contraseña:</strong> __SUPERADMIN_PASS__</div>
                                                                                                            </td>
                                                                                                        </tr>
                                                                <tr>
                                                                    <td style=\"background:#f8fbff;border:1px solid #d8e6fb;border-radius:10px;padding:14px 16px;\">
                                                                        <div style=\"font-size:14px;font-weight:800;color:#0b3f83;margin-bottom:8px;\">ADMIN</div>
                                                                        <div style=\"font-size:14px;line-height:1.65;\"><strong>Correo:</strong> __ADMIN_CORREO__</div>
                                                                        <div style=\"font-size:14px;line-height:1.65;\"><strong>Contraseña:</strong> __ADMIN_PASS__</div>
                                                                    </td>
                                                                </tr>
                                                                <tr>
                                                                    <td style=\"background:#f8fbff;border:1px solid #d8e6fb;border-radius:10px;padding:14px 16px;\">
                                                                        <div style=\"font-size:14px;font-weight:800;color:#0b3f83;margin-bottom:8px;\">USUARIO</div>
                                                                        <div style=\"font-size:14px;line-height:1.65;\"><strong>Correo:</strong> __USER_CORREO__</div>
                                                                        <div style=\"font-size:14px;line-height:1.65;\"><strong>Contraseña:</strong> __USER_PASS__</div>
                                                                    </td>
                                                                </tr>
                                                            </table>

                                                            <div style=\"margin-top:4px;padding:12px 14px;background:#fff8e6;border:1px solid #f7d778;border-radius:10px;font-size:14px;line-height:1.6;\">
                                                                <strong>Vigencia:</strong> hasta __VIGENCIA__<br/>
                                                                Estos accesos duran <strong>1 semana</strong> y luego se deshabilitan automáticamente.
                                                            </div>

                                                            <p style="margin:16px 0 0;">
                                                                <a href="__LOGIN_URL__" style="display:inline-block;background:#0b4a95;color:#ffffff;text-decoration:none;padding:10px 16px;border-radius:8px;font-size:13px;font-weight:700;">
                                                                    Ir a iniciar sesión
                                                                </a>
                                                            </p>

                                                            <p style=\"margin:16px 0 0;font-size:13px;line-height:1.6;color:#64748b;\">
                                                                Recomendación: no compartas este correo. Si necesitas una nueva demo, solicita otra desde el formulario.
                                                            </p>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style=\"padding:14px 24px 20px;font-size:12px;color:#94a3b8;\">
                                                            Equipo SmartMaint
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </body>
                                </html>
                                """;

                            return plantilla
                                .replace("__NOMBRE__", nombreSeguro)
                                .replace("__EMPRESA__", empresaSegura)
                                .replace("__SUPERADMIN_CORREO__", superAdminCorreoSeguro)
                                .replace("__SUPERADMIN_PASS__", superAdminContrasenaSegura)
                                .replace("__ADMIN_CORREO__", adminCorreoSeguro)
                                .replace("__ADMIN_PASS__", adminContrasenaSegura)
                                .replace("__USER_CORREO__", usuarioCorreoSeguro)
                                .replace("__USER_PASS__", usuarioContrasenaSegura)
                                .replace("__VIGENCIA__", vigenciaSegura)
                                .replace("__LOGIN_URL__", loginUrlSegura)
                                .replace("__LOGO_BLOCK__", bloqueLogo)
                                .replace("__LOGO_URL__", logoUrlSegura);
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