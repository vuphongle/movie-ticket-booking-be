package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.TokenConfirm;
import vn.edu.iuh.fit.entity.User;
import vn.edu.iuh.fit.model.enums.TokenType;

import java.util.Optional;


public interface TokenConfirmRepository extends JpaRepository<TokenConfirm, Integer> {
    Optional<TokenConfirm> findByTokenAndType(String token, TokenType tokenType);

    Optional<TokenConfirm> findFirstByUserAndTypeAndConfirmedDateIsNullOrderByExpiryDateDesc(User user, TokenType type);
}