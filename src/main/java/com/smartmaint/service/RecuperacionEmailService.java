package com.smartmaint.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "spring.mail",
        name = "host",
        matchIfMissing = false
)
public class RecuperacionEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(RecuperacionEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final String from;
    private final String smtpUsername;
    private final String loginUrl;

    public RecuperacionEmailService(

            ObjectProvider<JavaMailSender> mailSenderProvider,

            @Value("${app.purchase.mail.from:smartmaint.co@outlook.com}")
            String from,

            @Value("${spring.mail.username:}")
            String smtpUsername,

            @Value("${app.demo.login.url:https://smartmaint-frontend-last.vercel.app/login}")
            String loginUrl
    ) {

        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
        this.smtpUsername = smtpUsername;
        this.loginUrl = loginUrl;
    }

    public void enviarContrasenaTemporal(
            String correoDestino,
            String nombre,
            String contrasenaTemporal
    ) {

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {

            throw new IllegalStateException(
                    "El envío de correo no está configurado."
            );
        }

        String asunto = "Recuperación de contraseña - SmartMaint";

        String textoPlano =
                "Hola " + nombre + ",\n\n"
                        + "Recibimos una solicitud para restablecer tu contraseña en SmartMaint.\n"
                        + "Tu contraseña temporal es: " + contrasenaTemporal + "\n\n"
                        + "Al iniciar sesión con ella, deberás establecer una nueva contraseña.\n"
                        + "Iniciar sesión: " + loginUrl + "\n\n"
                        + "Si no solicitaste esto, ignora este correo.\n\n"
                        + "Equipo SmartMaint";

        String html = """
                <html lang="es">
                <body style="font-family:Segoe UI,Arial,sans-serif;background:#f3f7ff;padding:16px;">
                  <div style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #d7e4f8;">

                    <div style="background:linear-gradient(90deg,#0b3f83,#0a4a95 55%,#0f5db8);padding:16px 24px;color:#ffffff;font-weight:800;font-size:22px;letter-spacing:0.04em;">
                      SMARTMAINT
                    </div>

                    <div style="padding:28px 24px;">

                      <p style="margin:0 0 14px;font-size:1rem;">
                        Hola <strong>__NOMBRE__</strong>,
                      </p>

                      <p style="margin:0 0 14px;line-height:1.7;color:#334155;">
                        Recibimos una solicitud para restablecer tu contraseña en
                        <strong><em>SMARTMAINT</em></strong>.
                      </p>

                      <div style="background:#fff8e6;border:1px solid #f7d778;border-radius:10px;padding:16px 20px;margin-bottom:20px;">

                        <div style="font-weight:800;color:#0b3f83;margin-bottom:8px;font-size:0.85rem;text-transform:uppercase;letter-spacing:0.06em;">
                          Tu contraseña temporal
                        </div>

                        <div style="font-size:1.5rem;font-weight:800;color:#1e293b;letter-spacing:0.12em;">
                          __PASS__
                        </div>

                      </div>

                      <p style="margin:0 0 20px;line-height:1.7;color:#334155;">
                        Al iniciar sesión con esta contraseña, el sistema te pedirá que la cambies por una nueva antes de continuar.
                      </p>

                      <a href="__LOGIN_URL__"
                         style="display:inline-block;background:#0b4a95;color:#ffffff;text-decoration:none;padding:12px 24px;border-radius:8px;font-weight:700;font-size:0.95rem;">
                        Iniciar sesión
                      </a>

                      <p style="margin:24px 0 0;font-size:0.82rem;color:#94a3b8;">
                        Si no solicitaste este cambio, puedes ignorar este correo.
                      </p>

                    </div>
                  </div>
                </body>
                </html>
                """
                .replace("__NOMBRE__", escapar(nombre))
                .replace("__PASS__", escapar(contrasenaTemporal))
                .replace("__LOGIN_URL__", escapar(loginUrl));

        try {

            MimeMessage msg = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(
                    new InternetAddress(
                            resolverRemitente(),
                            "SmartMaint Notificaciones",
                            "UTF-8"
                    )
            );

            helper.setReplyTo(resolverRemitente());

            helper.setTo(correoDestino);

            helper.setSubject(asunto);

            helper.setText(textoPlano, html);

            log.info(
                    "Enviando correo de recuperación a {} desde {}",
                    correoDestino,
                    resolverRemitente()
            );

            mailSender.send(msg);

            log.info(
                    "Correo de recuperación enviado correctamente a {}",
                    correoDestino
            );

        } catch (Exception e) {

            log.error(
                    "Error enviando recuperación a {}: {}",
                    correoDestino,
                    e.getMessage(),
                    e
            );

            throw new IllegalStateException(
                    "No se pudo enviar el correo de recuperación",
                    e
            );
        }
    }

    private String resolverRemitente() {

        if (from != null && !from.isBlank()) {
            return from;
        }

        if (smtpUsername != null && !smtpUsername.isBlank()) {
            return smtpUsername;
        }

        throw new IllegalStateException(
                "No hay remitente configurado para correo."
        );
    }

    private String escapar(String v) {

        if (v == null) {
            return "";
        }

        return v.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}