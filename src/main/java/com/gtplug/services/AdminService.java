package com.gtplug.services;

import com.gtplug.models.Admin;
import com.gtplug.models.User;
import com.gtplug.repositories.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final AdminRepository adminRepository;
    private final UserService userService;

    public AdminService() {
        this.adminRepository = new AdminRepository();
        this.userService = new UserService();
    }

    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }

    public Optional<Admin> findByUserId(Long userId) {
        return adminRepository.findByUserId(userId);
    }

    public Admin createAdmin(String username, String email, String password, 
                             String fullName, String mobileNumber, String department) {
        // Create user account
        User user = userService.createUser(username, email, password, User.UserRole.ADMIN);

        // Create admin profile
        Admin admin = new Admin(user.getId(), fullName, mobileNumber, department);
        admin = adminRepository.save(admin);
        admin.setUser(user);

        logger.info("Admin created successfully: {}", username);
        return admin;
    }

    public Admin updateAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    public void deleteAdmin(Long id) {
        Optional<Admin> adminOpt = adminRepository.findById(id);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            adminRepository.delete(id);
            userService.deleteUser(admin.getUserId());
            logger.info("Admin deleted: {}", id);
        }
    }
}
