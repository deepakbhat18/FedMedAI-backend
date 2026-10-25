package com.fedlearn.medical.repository;

import com.fedlearn.medical.entity.RefreshToken;
import com.fedlearn.medical.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    @Modifying
    int deleteByUser(User user);

    @Modifying
    int deleteByRevokedTrue();
}
