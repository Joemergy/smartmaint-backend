package com.smartmaint.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DemoEmailService {

    private static final Logger log = LoggerFactory.getLogger(DemoEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;
    private final String loginUrl;
    private final String logoUrl;

    public DemoEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.demo.mail.from:no-reply@smartmaint.com}") String from,
            @Value("${app.demo.login.url:http://localhost:3000/login}") String loginUrl,
            @Value("${app.demo.mail.logo.url:}") String logoUrl
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
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

        // Si NO hay SMTP configurado, simplemente no envía correo
        // pero NO rompe la aplicación.
        if (mailSender == null) {
            log.warn("SMTP no configurado. Se omitió envío de correo demo a {}", destinatario);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String vigenciaFormateada = expiraEn.format(formatter);

        String textoPlano =
                "Hola " + nombre + ",\n\n" +
                "Tu solicitud demo para " + empresa + " fue aprobada.\n\n" +

                "SUPERADMIN\n" +
                "Correo: " + superAdminCorreo + "\n" +
                "Contraseña: " + superAdminContrasena + "\n\n" +

                "ADMIN\n" +
                "Correo: " + adminCorreo + "\n" +
                "Contraseña: " + adminContrasena + "\n\n" +

                "USUARIO\n" +
                "Correo: " + usuarioCorreo + "\n" +
                "Contraseña: " + usuarioContrasena + "\n\n" +

                "Vigencia: hasta " + vigenciaFormateada + "\n\n" +

                "Iniciar sesión: " + loginUrl + "\n\n" +

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

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(
                    new InternetAddress(
                            from,
                            "SmartMaint Notificaciones",
                            "UTF-8"
                    )
            );

            helper.setReplyTo("no-reply@smartmaint.com");

            helper.setTo(destinatario);

            helper.setSubject("Tus credenciales demo - SmartMaint");

            helper.setText(textoPlano, html);

            mailSender.send(mimeMessage);

            log.info("Correo demo enviado correctamente a {}", destinatario);

        } catch (MessagingException | UnsupportedEncodingException e) {

            log.error("No se pudo construir correo demo", e);

        } catch (Exception e) {

            log.error("Error enviando correo demo", e);
        }
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

        String logoHtml = "";

        if (logoUrl != null && !logoUrl.isBlank()) {
            logoHtml = """
                    <img src="%s"
                         alt="SMARTMAINT"
                         style="max-height:40px;margin-bottom:20px;">
                    """.formatted(escaparHtml(logoUrl));
        }

        return """
                <html lang="es">
                <body style="font-family:Segoe UI,Arial,sans-serif;background:#f3f7ff;padding:20px;">

                    <div style="
                        max-width:700px;
                        margin:auto;
                        background:white;
                        border-radius:14px;
                        overflow:hidden;
                        border:1px solid #d7e4f8;
                    ">

                        <div style="
                            background:linear-gradient(90deg,#0b3f83,#0f5db8);
                            color:white;
                            padding:20px;
                            font-size:24px;
                            font-weight:800;
                        ">
                            SMARTMAINT
                        </div>

                        <div style="padding:30px;">

                            %s

                            <p>Hola <strong>%s</strong>,</p>

                            <p>
                                Tu solicitud demo para
                                <strong>%s</strong>
                                fue aprobada.
                            </p>

                            <h3>SUPERADMIN</h3>
                            <p><strong>Correo:</strong> %s</p>
                            <p><strong>Contraseña:</strong> %s</p>

                            <h3>ADMIN</h3>
                            <p><strong>Correo:</strong> %s</p>
                            <p><strong>Contraseña:</strong> %s</p>

                            <h3>USUARIO</h3>
                            <p><strong>Correo:</strong> %s</p>
                            <p><strong>Contraseña:</strong> %s</p>

                            <div style="
                                background:#fff8e6;
                                border:1px solid #f7d778;
                                border-radius:10px;
                                padding:16px;
                                margin-top:24px;
                            ">
                                <strong>Vigencia:</strong> %s
                            </div>

                            <p style="margin-top:24px;">
                                <a href="%s"
                                   style="
                                     background:#0b4a95;
                                     color:white;
                                     padding:12px 22px;
                                     text-decoration:none;
                                     border-radius:8px;
                                     font-weight:700;
                                   ">
                                    Iniciar sesión
                                </a>
                            </p>

                        </div>
                    </div>

                </body>
                </html>
                """.formatted(
                logoHtml,
                escaparHtml(nombre),
                escaparHtml(empresa),

                escaparHtml(superAdminCorreo),
                escaparHtml(superAdminContrasena),

                escaparHtml(adminCorreo),
                escaparHtml(adminContrasena),

                escaparHtml(usuarioCorreo),
                escaparHtml(usuarioContrasena),

                escaparHtml(vigenciaFormateada),
                escaparHtml(loginUrl)
        );
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