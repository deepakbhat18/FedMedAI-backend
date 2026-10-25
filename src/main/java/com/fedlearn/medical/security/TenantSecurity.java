package com.fedlearn.medical.security;

import com.fedlearn.medical.entity.User;
import com.fedlearn.medical.enums.Role;
import com.fedlearn.medical.repository.HospitalRepository;
import com.fedlearn.medical.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Component;

@Component("tenantSecurity")
public class TenantSecurity {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    public boolean hasHospitalAccess(Authentication authentication, Long hospitalId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !user.getIsActive()) {
            return false;
        }

        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        if (user.getRole() == Role.HOSPITAL_ADMIN) {
            return hospitalRepository.findByAdminUserUsername(username)
                    .map(hospital -> hospital.getId().equals(hospitalId))
                    .orElse(false);
        }

        return false;
    }
}
