package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.HospitalEmailRequest;
import com.fedlearn.medical.entity.Hospital;
import com.fedlearn.medical.entity.Patient;
import com.fedlearn.medical.exception.BadRequestException;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.HospitalRepository;
import com.fedlearn.medical.repository.PatientRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger =
            LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    @Value("${app.email.from}")
    private String systemEmail;

    @Value("${app.email.from-name}")
    private String systemName;

    @Value("${app.ml-service.base-url}")
    private String mlServiceBaseUrl;

    public Map<String, Object> sendReportFromHospital(
            Long hospitalId,
            HospitalEmailRequest request) {

        logger.info("Sending report from hospital: {} to patient email: {}",
                hospitalId, request.getPatientEmail());

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hospital", "id", hospitalId));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient", "id", request.getPatientId()));

        if (!patient.getHospital().getId().equals(hospitalId)) {
            throw new BadRequestException(
                    "Patient does not belong to this hospital");
        }

        logger.info("Generating PDF for patient: {}", patient.getPatientCode());
        byte[] pdfBytes = generatePDF(hospital, patient, request);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BadRequestException(
                    "Failed to generate PDF report. " +
                            "Make sure ML service is running.");
        }

        try {
            sendEmailFromHospital(
                    hospital,
                    patient,
                    request,
                    pdfBytes
            );

            logger.info("Report sent successfully from {} to {}",
                    hospital.getName(), request.getPatientEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Medical report sent successfully");
            response.put("sentFrom", hospital.getName());
            response.put("sentTo", request.getPatientEmail());
            response.put("patientCode", patient.getPatientCode());
            response.put("hospitalName", hospital.getName());
            response.put("pdfSizeBytes", pdfBytes.length);
            response.put("sentAt", LocalDateTime.now().toString());
            return response;

        } catch (MessagingException e) {
            logger.error("Failed to send email: {}", e.getMessage());
            throw new BadRequestException(
                    "Failed to send email: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            logger.error("Email encoding error: {}", e.getMessage());
            throw new BadRequestException(
                    "Email encoding error: " + e.getMessage());
        }
    }
    private byte[] generatePDF(
            Hospital hospital,
            Patient patient,
            HospitalEmailRequest request) {
        try {
            Map<String, Object> patientData = new java.util.LinkedHashMap<>();
            patientData.put("patientCode", patient.getPatientCode());
            patientData.put("age", patient.getAge());
            patientData.put("gender", patient.getGender() != null
                    ? patient.getGender().name() : "Unknown");
            patientData.put("hospitalName", hospital.getName());
            patientData.put("diagnosis", patient.getDiagnosis());

            Map<String, Object> hospitalData = new java.util.LinkedHashMap<>();
            hospitalData.put("name", hospital.getName());
            hospitalData.put("code", hospital.getCode());
            hospitalData.put("contactEmail", hospital.getContactEmail());
            hospitalData.put("contactPhone", hospital.getContactPhone());
            hospitalData.put("address", hospital.getAddress());

            Map<String, Object> payload = new HashMap<>();
            payload.put("patient_data", patientData);
            payload.put("hospital_data", hospitalData);
            payload.put("predictions", request.getPredictions());

            if (request.getRecommendations() != null) {
                payload.put("recommendations", request.getRecommendations());
            }
            if (request.getFeatures() != null) {
                payload.put("features", request.getFeatures());
            }
            org.springframework.http.HttpHeaders headers =
                    new org.springframework.http.HttpHeaders();
            headers.setContentType(
                    org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(payload, headers);

            org.springframework.http.ResponseEntity<byte[]> response =
                    new org.springframework.web.client.RestTemplate().exchange(
                            mlServiceBaseUrl + "/ml/report/generate",
                            org.springframework.http.HttpMethod.POST,
                            entity,
                            byte[].class
                    );

            return response.getBody();

        } catch (Exception e) {
            logger.error("PDF generation failed: {}", e.getMessage());
            throw new BadRequestException(
                    "PDF generation failed: " + e.getMessage());
        }
    }
    private void sendEmailFromHospital(
            Hospital hospital,
            Patient patient,
            HospitalEmailRequest request,
            byte[] pdfBytes)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );
        try {
            helper.setFrom(new InternetAddress(
                    systemEmail,
                    hospital.getName(),
                    StandardCharsets.UTF_8.name()
            ));
        } catch (UnsupportedEncodingException e) {
            helper.setFrom(systemEmail);
            logger.warn("Could not set hospital name as sender: {}",
                    e.getMessage());
        }

        if (hospital.getContactEmail() != null
                && !hospital.getContactEmail().isBlank()) {
            helper.setReplyTo(hospital.getContactEmail());
        }


        helper.setTo(request.getPatientEmail());

        String subject = (request.getSubject() != null
                && !request.getSubject().isBlank())
                ? request.getSubject()
                : "Your Medical Diagnosis Report - " + hospital.getName();
        helper.setSubject(subject);


        String patientName = (request.getPatientName() != null
                && !request.getPatientName().isBlank())
                ? request.getPatientName()
                : "Patient " + patient.getPatientCode();

        helper.setText(
                buildHospitalEmailBody(hospital, patient, patientName),
                true
        );

        String filename = "medical_report_"
                + patient.getPatientCode()
                + "_"
                + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
                + ".pdf";

        helper.addAttachment(
                filename,
                new org.springframework.core.io.ByteArrayResource(pdfBytes),
                "application/pdf"
        );

        mailSender.send(message);
        logger.info("Email sent from {} to {} | File: {}",
                hospital.getName(),
                request.getPatientEmail(),
                filename);
    }

    private String buildHospitalEmailBody(
            Hospital hospital,
            Patient patient,
            String patientName) {

        String hospitalName = hospital.getName();
        String hospitalAddress = hospital.getAddress() != null
                ? hospital.getAddress() : "";
        String hospitalPhone = hospital.getContactPhone() != null
                ? hospital.getContactPhone() : "";
        String hospitalEmail = hospital.getContactEmail() != null
                ? hospital.getContactEmail() : "";
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        String patientCode = patient.getPatientCode();

        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" " +
                "content=\"width=device-width,initial-scale=1.0\">" +
                "</head>" +
                "<body style=\"margin:0;padding:0;background:#f0f4f8;" +
                "font-family:'Segoe UI',Arial,sans-serif;\">" +


                "<div style=\"max-width:600px;margin:32px auto;" +
                "background:#ffffff;border-radius:16px;" +
                "overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.1);\">" +


                "<div style=\"background:linear-gradient(" +
                "135deg,#1a56db,#7e3af2);padding:36px 40px;" +
                "text-align:center;\">" +
                "<p style=\"color:rgba(255,255,255,0.8);margin:0 0 4px;" +
                "font-size:12px;letter-spacing:2px;text-transform:uppercase;\">" +
                "Medical Diagnosis Report</p>" +
                "<h1 style=\"color:white;margin:0;font-size:26px;" +
                "font-weight:700;\">" +
                "🏥 " + hospitalName + "</h1>" +
                (hospitalAddress.isBlank() ? "" :
                        "<p style=\"color:rgba(255,255,255,0.7);margin:8px 0 0;" +
                                "font-size:13px;\">" + hospitalAddress + "</p>") +
                "</div>" +


                "<div style=\"padding:36px 40px;\">" +


                "<p style=\"color:#111827;font-size:16px;margin:0 0 8px;" +
                "font-weight:600;\">Dear " + patientName + ",</p>" +
                "<p style=\"color:#6b7280;font-size:14px;line-height:1.7;" +
                "margin:0 0 28px;\">" +
                "Please find attached your personalized AI-powered medical " +
                "diagnosis report from <strong>" + hospitalName + "</strong>. " +
                "This report was generated using our Federated Learning system " +
                "and contains detailed health risk assessments across multiple " +
                "disease categories along with personalized medical recommendations." +
                "</p>" +


                "<div style=\"background:#f9fafb;border-radius:12px;" +
                "padding:24px;margin:0 0 24px;" +
                "border:1px solid #e5e7eb;\">" +
                "<h3 style=\"color:#111827;font-size:14px;font-weight:600;" +
                "margin:0 0 16px;text-transform:uppercase;" +
                "letter-spacing:0.5px;\">Report Details</h3>" +
                "<table style=\"width:100%;border-collapse:collapse;\">" +


                "<tr><td style=\"padding:8px 0;color:#6b7280;" +
                "font-size:13px;width:45%;\">Patient Code</td>" +
                "<td style=\"padding:8px 0;color:#111827;font-size:13px;" +
                "font-weight:600;\">" + patientCode + "</td></tr>" +


                "<tr><td style=\"padding:8px 0;color:#6b7280;" +
                "font-size:13px;\">Hospital</td>" +
                "<td style=\"padding:8px 0;color:#111827;" +
                "font-size:13px;font-weight:600;\">" +
                hospitalName + "</td></tr>" +


                "<tr><td style=\"padding:8px 0;color:#6b7280;" +
                "font-size:13px;\">Report Date</td>" +
                "<td style=\"padding:8px 0;color:#111827;" +
                "font-size:13px;font-weight:600;\">" + date + "</td></tr>" +


                "<tr><td style=\"padding:8px 0;color:#6b7280;" +
                "font-size:13px;\">Diseases Screened</td>" +
                "<td style=\"padding:8px 0;color:#1a56db;" +
                "font-size:13px;font-weight:600;\">" +
                "5 Conditions</td></tr>" +


                "<tr><td style=\"padding:8px 0;color:#6b7280;" +
                "font-size:13px;\">Technology</td>" +
                "<td style=\"padding:8px 0;color:#7e3af2;" +
                "font-size:13px;font-weight:600;\">" +
                "Federated Learning AI</td></tr>" +

                "</table></div>" +


                "<div style=\"margin:0 0 28px;\">" +
                "<p style=\"color:#6b7280;font-size:12px;margin:0 0 12px;\">" +
                "Conditions analyzed in this report:</p>" +
                "<div style=\"display:flex;flex-wrap:wrap;gap:8px;\">" +
                diseaseBadge("❤️", "Heart Disease", "#fee2e2", "#dc2626") +
                diseaseBadge("🩸", "Diabetes", "#fef3c7", "#d97706") +
                diseaseBadge("🫘", "Kidney Disease", "#dbeafe", "#1d4ed8") +
                diseaseBadge("🫁", "Liver Disease", "#d1fae5", "#065f46") +
                diseaseBadge("🧠", "Stroke", "#ede9fe", "#6d28d9") +
                "</div></div>" +

                "<div style=\"background:#fffbeb;border:1px solid #fcd34d;" +
                "border-radius:10px;padding:16px;margin:0 0 28px;\">" +
                "<p style=\"color:#92400e;font-size:12px;margin:0;" +
                "line-height:1.6;\">" +
                "<strong>⚕️ Important Medical Disclaimer:</strong> " +
                "This report is generated by an AI-powered system and is " +
                "intended to assist medical professionals in diagnosis. " +
                "It does <strong>NOT</strong> replace professional medical " +
                "advice, diagnosis, or treatment. Please consult your " +
                "doctor or healthcare provider for proper medical guidance." +
                "</p></div>" +

                "<div style=\"background:#eff6ff;border-radius:10px;" +
                "padding:16px;margin:0 0 28px;" +
                "border-left:4px solid #1a56db;\">" +
                "<p style=\"color:#1e40af;font-size:13px;margin:0;" +
                "font-weight:600;\">📋 Recommended Next Steps:</p>" +
                "<ul style=\"color:#3b82f6;font-size:12px;margin:8px 0 0;" +
                "padding-left:20px;line-height:1.8;\">" +
                "<li>Review the attached PDF report carefully</li>" +
                "<li>Schedule a follow-up appointment with your doctor</li>" +
                "<li>Bring this report to your next medical consultation</li>" +
                "<li>Contact the hospital if you have any questions</li>" +
                "</ul></div>" +


                "<p style=\"color:#374151;font-size:13px;margin:0 0 4px;\">" +
                "Best regards,</p>" +
                "<p style=\"color:#111827;font-size:15px;font-weight:700;" +
                "margin:0 0 4px;\">" + hospitalName + "</p>" +
                "<p style=\"color:#6b7280;font-size:12px;margin:0;\">" +
                "Medical Team" +
                (hospitalPhone.isBlank() ? "" : " · " + hospitalPhone) +
                "</p>" +
                "</div>" +


                "<div style=\"background:#f9fafb;padding:20px 40px;" +
                "text-align:center;border-top:1px solid #e5e7eb;\">" +


                (!hospitalEmail.isBlank() ?
                        "<p style=\"color:#6b7280;font-size:12px;margin:0 0 8px;\">" +
                                "Questions? Contact us: " +
                                "<a href=\"mailto:" + hospitalEmail + "\" " +
                                "style=\"color:#1a56db;text-decoration:none;\">" +
                                hospitalEmail + "</a></p>" : "") +

                "<p style=\"color:#9ca3af;font-size:11px;margin:0;\">" +
                "This is an automated message from " + hospitalName + ". " +
                "Please do not reply directly to this email.<br>" +
                "© " + java.time.Year.now().getValue() + " " +
                hospitalName + ". All rights reserved." +
                "</p></div>" +

                "</div>" +
                "</body></html>";
    }

    private String diseaseBadge(
            String emoji,
            String name,
            String bgColor,
            String textColor) {
        return "<span style=\"display:inline-block;background:" +
                bgColor + ";color:" + textColor +
                ";padding:4px 12px;border-radius:20px;" +
                "font-size:12px;font-weight:500;\">" +
                emoji + " " + name + "</span>";
    }

    public void sendTrainingCompletedNotification(
            String toEmail,
            String hospitalName,
            Map<String, Object> trainingResult) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            try {
                helper.setFrom(new InternetAddress(
                        systemEmail,
                        systemName,
                        StandardCharsets.UTF_8.name()
                ));
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(systemEmail);
            }

            helper.setTo(toEmail);
            helper.setSubject(
                    "FL Training Complete - New Model Available");

            String round = String.valueOf(
                    trainingResult.getOrDefault("trainingRound", "N/A"));
            String accuracy = String.valueOf(
                    trainingResult.getOrDefault("globalAccuracy", "N/A"));
            String version = String.valueOf(
                    trainingResult.getOrDefault("modelVersion", "N/A"));

            String body = "FL Training Round " + round + " completed.\n\n" +
                    "Model Version: " + version + "\n" +
                    "Global Accuracy: " + accuracy + "\n\n" +
                    "Log in to the FL Medical System to view results.\n\n" +
                    "FL Medical System";

            helper.setText(body, false);
            mailSender.send(mimeMessage);
            logger.info("Training notification sent to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send training notification: {}",
                    e.getMessage());
        }
    }

    public void sendSimpleNotification(
            String toEmail,
            String subject,
            String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            try {
                helper.setFrom(new InternetAddress(
                        systemEmail,
                        systemName,
                        StandardCharsets.UTF_8.name()
                ));
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(systemEmail);
            }

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(message, false);
            mailSender.send(mimeMessage);
            logger.info("Notification sent to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send notification: {}",
                    e.getMessage());
        }
    }
}