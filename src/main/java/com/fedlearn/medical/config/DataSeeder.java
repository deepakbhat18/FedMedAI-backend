
package com.fedlearn.medical.config;

import com.fedlearn.medical.entity.*;
import com.fedlearn.medical.enums.*;
import com.fedlearn.medical.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            logger.info("Data already seeded. Skipping...");
            return;
        }

        logger.info("=== SEEDING INITIAL DATA ===");

        // 1. Create Admin User
        User admin = User.builder()
                .username("admin")
                .email("admin@fedlearn.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        admin = userRepository.save(admin);
        logger.info("Created admin user: admin/admin123");

        // 2. Create Hospital Admin Users
        User hospitalAdmin1 = User.builder()
                .username("hospital1_admin")
                .email("admin@hospital1.com")
                .passwordHash(passwordEncoder.encode("hospital123"))
                .fullName("Dr. Sarah Johnson")
                .role(Role.HOSPITAL_ADMIN)
                .isActive(true)
                .build();
        hospitalAdmin1 = userRepository.save(hospitalAdmin1);

        User hospitalAdmin2 = User.builder()
                .username("hospital2_admin")
                .email("admin@hospital2.com")
                .passwordHash(passwordEncoder.encode("hospital123"))
                .fullName("Dr. Michael Chen")
                .role(Role.HOSPITAL_ADMIN)
                .isActive(true)
                .build();
        hospitalAdmin2 = userRepository.save(hospitalAdmin2);

        User hospitalAdmin3 = User.builder()
                .username("hospital3_admin")
                .email("admin@hospital3.com")
                .passwordHash(passwordEncoder.encode("hospital123"))
                .fullName("Dr. Emily Davis")
                .role(Role.HOSPITAL_ADMIN)
                .isActive(true)
                .build();
        hospitalAdmin3 = userRepository.save(hospitalAdmin3);

        logger.info("Created 3 hospital admin users");

        // 3. Create Hospitals
        Hospital hospital1 = Hospital.builder()
                .name("City General Hospital")
                .code("CGH-001")
                .address("123 Medical Ave, New York, NY 10001")
                .contactEmail("info@citygeneral.com")
                .contactPhone("+1-555-0100")
                .adminUser(hospitalAdmin1)
                .status(HospitalStatus.ACTIVE)
                .build();
        hospital1 = hospitalRepository.save(hospital1);

        Hospital hospital2 = Hospital.builder()
                .name("Metro Heart Center")
                .code("MHC-002")
                .address("456 Cardiac Blvd, Boston, MA 02101")
                .contactEmail("info@metroheart.com")
                .contactPhone("+1-555-0200")
                .adminUser(hospitalAdmin2)
                .status(HospitalStatus.ACTIVE)
                .build();
        hospital2 = hospitalRepository.save(hospital2);

        Hospital hospital3 = Hospital.builder()
                .name("Valley Medical Institute")
                .code("VMI-003")
                .address("789 Health St, San Francisco, CA 94101")
                .contactEmail("info@valleymedical.com")
                .contactPhone("+1-555-0300")
                .adminUser(hospitalAdmin3)
                .status(HospitalStatus.ACTIVE)
                .build();
        hospital3 = hospitalRepository.save(hospital3);

        logger.info("Created 3 hospitals");

        // 4. Create Sample Patients for each hospital
        Random random = new Random(42);
        int patientCount = 0;

        Hospital[] hospitals = {hospital1, hospital2, hospital3};
        for (Hospital hospital : hospitals) {
            for (int i = 1; i <= 20; i++) {
                // Generate realistic heart disease features
                int age = 30 + random.nextInt(45);
                int sex = random.nextInt(2);
                double bp = 100 + random.nextInt(80);
                double cholesterol = 150 + random.nextInt(150);
                double heartRate = 60 + random.nextInt(80);
                double bmi = 18 + random.nextDouble() * 20;
                double glucose = 70 + random.nextInt(100);
                int smoking = random.nextInt(2);
                int diabetes = random.nextInt(2);
                int familyHistory = random.nextInt(2);

                // Simple rule for generating labels
                double riskScore = 0;
                if (bp > 140) riskScore += 0.2;
                if (cholesterol > 240) riskScore += 0.2;
                if (bmi > 30) riskScore += 0.15;
                if (smoking == 1) riskScore += 0.15;
                if (diabetes == 1) riskScore += 0.15;
                if (familyHistory == 1) riskScore += 0.15;
                int label = riskScore >= 0.4 ? 1 : 0;

                Map<String, Object> features = new LinkedHashMap<>();
                features.put("bloodPressure", bp);
                features.put("cholesterol", cholesterol);
                features.put("heartRate", heartRate);
                features.put("bmi", Math.round(bmi * 10.0) / 10.0);
                features.put("glucose", glucose);
                features.put("smoking", smoking);
                features.put("diabetes", diabetes);
                features.put("familyHistory", familyHistory);

                Patient patient = Patient.builder()
                        .hospital(hospital)
                        .patientCode("PAT-" + hospital.getCode() + "-"
                                + String.format("%03d", i))
                        .age(age)
                        .gender(sex == 1 ? Gender.MALE : Gender.FEMALE)
                        .features(features)
                        .diagnosis(label == 1 ? "Heart Disease Risk" : "Healthy")
                        .actualLabel(label)
                        .build();

                patientRepository.save(patient);
                patientCount++;
            }
        }

        logger.info("Created {} sample patients across 3 hospitals", patientCount);
        logger.info("=== DATA SEEDING COMPLETE ===");
        logger.info("");
        logger.info("========================================");
        logger.info("  DEFAULT CREDENTIALS:");
        logger.info("  Admin:    admin / admin123");
        logger.info("  Hospital1: hospital1_admin / hospital123");
        logger.info("  Hospital2: hospital2_admin / hospital123");
        logger.info("  Hospital3: hospital3_admin / hospital123");
        logger.info("========================================");
        logger.info("  Swagger UI: http://localhost:8080/swagger-ui.html");
        logger.info("========================================");
    }
}