package FinanceManangementSystem.demo.Init;

import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Model.UserAddress;
import FinanceManangementSystem.demo.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CreateAdminIfNot implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Default admin credentials - override these via environment variables in Render:
    //   DEFAULT_ADMIN_NAME, DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_EMAIL,
    //   DEFAULT_ADMIN_PASSWORD, DEFAULT_ADMIN_MOBILE
    @Value("${app.default-admin.owner-name:Admin}")
    private String adminOwnerName;

    @Value("${app.default-admin.username:admin}")
    private String adminUsername;

    @Value("${app.default-admin.email:${DEFAULT_ADMIN_EMAIL:admin@financems.app}}")
    private String adminEmail;

    @Value("${app.default-admin.password:${DEFAULT_ADMIN_PASSWORD:ChangeMe@2025!}}")
    private String adminPassword;

    @Value("${app.default-admin.mobile:${DEFAULT_ADMIN_MOBILE:0000000000}}")
    private String adminMobile;

    public CreateAdminIfNot(UserRepository userRepository,
                            BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        log.info("DEFAULT ADMIN - checking default admin...");

        if (userRepository.count() == 0) {

            User admin = new User();

            admin.setOwnerName(adminOwnerName);
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setMobileNumber(adminMobile);
            admin.setRole(UserRole.ADMIN);

            // Minimal address - update via Profile after first login
            UserAddress address = new UserAddress();
            address.setCity("Surat");
            address.setState("Gujarat");
            // @PrePersist will automatically set country to "India"
            address.setUser(admin);
            admin.setAddress(address);

            userRepository.save(admin);

            log.info("✅ Default Admin Created Successfully. IMPORTANT: Change the default admin password immediately after first login.");
        }
    }
}