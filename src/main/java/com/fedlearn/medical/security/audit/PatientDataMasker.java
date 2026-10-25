package com.fedlearn.medical.security.audit;

public class PatientDataMasker {

    public static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "***";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder masked = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() <= 1) {
                masked.append(part).append("*");
            } else {
                masked.append(part.charAt(0))
                      .append("*".repeat(Math.max(1, part.length() - 2)))
                      .append(part.length() > 2 ? part.charAt(part.length() - 1) : "*");
            }
            if (i < parts.length - 1) masked.append(" ");
        }

        return masked.toString();
    }

    public static String maskPatientCode(String patientCode) {
        if (patientCode == null || patientCode.isBlank()) return "PAT-***";
        if (!patientCode.contains("-")) {
            if (patientCode.length() <= 4) return "****";
            return "***" + patientCode.substring(patientCode.length() - 4);
        }
        String[] parts = patientCode.split("-", 2);
        String prefix = parts[0];
        String suffix = parts[1];
        if (suffix.length() <= 4) {
            return prefix + "-***" + suffix;
        }
        return prefix + "-***" + suffix.substring(suffix.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***@***.com";
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "*@" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }
}
