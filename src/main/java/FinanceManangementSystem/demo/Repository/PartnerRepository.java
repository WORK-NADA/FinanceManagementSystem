package FinanceManangementSystem.demo.Repository;

import FinanceManangementSystem.demo.Model.Partner;
import FinanceManangementSystem.demo.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerRepository
        extends JpaRepository<Partner, Long> {

    Optional<Partner> findByPublicId(
            UUID publicId
    );

    Optional<Partner> findByUserAndPublicId(User user, UUID publicId);

    Optional<Partner> findByUserAndPublicIdAndIsActiveTrue(User user, UUID publicId);

    List<Partner> findByUser(User user);

    List<Partner> findByUserAndIsActiveTrue(User user);

    Optional<Partner> findByPublicIdAndIsActiveTrue(
            UUID publicId
    );

    List<Partner> findByIsActiveTrue();

    boolean existsByMobileNumber(
            String mobileNumber
    );

    boolean existsByUserAndMobileNumber(
            User user,
            String mobileNumber
    );

    boolean existsByMobileNumberAndPublicIdNot(
            String mobileNumber,
            UUID publicId
    );

    boolean existsByUserAndMobileNumberAndPublicIdNot(
            User user,
            String mobileNumber,
            UUID publicId
    );

    @Query("SELECT COALESCE(SUM(p.sharePercentage), 0) FROM Partner p WHERE p.isActive = true")
    BigDecimal sumActiveSharePercentage();

    @Query("SELECT COALESCE(SUM(p.sharePercentage), 0) FROM Partner p WHERE p.user = :user AND p.isActive = true")
    BigDecimal sumActiveSharePercentage(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(p.sharePercentage), 0) FROM Partner p WHERE p.isActive = true AND p.publicId <> :publicId")
    BigDecimal sumActiveSharePercentageExcluding(
            @Param("publicId") UUID publicId
    );

    @Query("SELECT COALESCE(SUM(p.sharePercentage), 0) FROM Partner p WHERE p.user = :user AND p.isActive = true AND p.publicId <> :publicId")
    BigDecimal sumActiveSharePercentageExcluding(
            @Param("user") User user,
            @Param("publicId") UUID publicId
    );
}
