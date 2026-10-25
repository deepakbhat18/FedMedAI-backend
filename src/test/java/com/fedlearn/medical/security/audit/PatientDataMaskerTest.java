package com.fedlearn.medical.security.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientDataMaskerTest {

    @Test
    @DisplayName("Should correctly mask full names while keeping first and last characters")
    void maskName_Success() {
        assertEquals("J**n D*e", PatientDataMasker.maskName("John Doe"));
        assertEquals("A***e M***a S***h", PatientDataMasker.maskName("Alice Maria Smith"));
    }

    @Test
    @DisplayName("Should correctly mask patient code identifiers")
    void maskPatientCode_Success() {
        assertEquals("PAT-***5678", PatientDataMasker.maskPatientCode("PAT-12345678"));
        assertEquals("HOSP-***9999", PatientDataMasker.maskPatientCode("HOSP-88889999"));
    }

    @Test
    @DisplayName("Should correctly mask email addresses")
    void maskEmail_Success() {
        assertEquals("j***e@hospital.com", PatientDataMasker.maskEmail("john.doe@hospital.com"));
        assertEquals("a***e@clinic.org", PatientDataMasker.maskEmail("alice@clinic.org"));
    }

    @Test
    @DisplayName("Should handle null or empty inputs gracefully without throwing NullPointerException")
    void mask_NullOrEmpty_HandlesGracefully() {
        assertNotNull(PatientDataMasker.maskName(null));
        assertNotNull(PatientDataMasker.maskPatientCode(null));
        assertNotNull(PatientDataMasker.maskEmail(null));
    }
}
