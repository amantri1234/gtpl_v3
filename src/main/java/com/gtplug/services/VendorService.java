package com.gtplug.services;

import com.gtplug.models.User;
import com.gtplug.models.Vendor;
import com.gtplug.repositories.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class VendorService {
    private static final Logger logger = LoggerFactory.getLogger(VendorService.class);
    private final VendorRepository vendorRepository;
    private final UserService userService;

    public VendorService() {
        this.vendorRepository = new VendorRepository();
        this.userService = new UserService();
    }

    public Optional<Vendor> findById(Long id) {
        return vendorRepository.findById(id);
    }

    public Optional<Vendor> findByUserId(Long userId) {
        return vendorRepository.findByUserId(userId);
    }

    public boolean existsByMobileNumber(String mobileNumber) {
        return vendorRepository.existsByMobileNumber(mobileNumber);
    }

    public VendorRegistrationResult registerVendor(String vendorName, String username, String email, 
                                                    String mobileNumber, String password) {
        try {
            // Validate inputs
            if (vendorName == null || vendorName.trim().isEmpty()) {
                return VendorRegistrationResult.failure("Vendor name is required");
            }
            if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
                return VendorRegistrationResult.failure("Mobile number is required");
            }
            if (!mobileNumber.matches("^[0-9]{10,15}$")) {
                return VendorRegistrationResult.failure("Invalid mobile number format");
            }
            if (existsByMobileNumber(mobileNumber)) {
                return VendorRegistrationResult.failure("Mobile number already registered");
            }

            // Create user account
            User user = userService.createUser(username, email, password, User.UserRole.VENDOR);

            // Create vendor profile
            Vendor vendor = new Vendor(user.getId(), vendorName, mobileNumber, LocalDate.now());
            vendor = vendorRepository.save(vendor);
            vendor.setUser(user);

            logger.info("Vendor registered successfully: {}", username);
            return VendorRegistrationResult.success(vendor);

        } catch (IllegalArgumentException e) {
            return VendorRegistrationResult.failure(e.getMessage());
        } catch (Exception e) {
            logger.error("Error registering vendor: {}", e.getMessage());
            return VendorRegistrationResult.failure("Registration failed. Please try again.");
        }
    }

    public Vendor updateVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    public List<Vendor> findAll(int page, int size, String search) {
        return vendorRepository.findAll(page, size, search);
    }

    public long count(String search) {
        return vendorRepository.count(search);
    }

    public void deleteVendor(Long id) {
        Optional<Vendor> vendorOpt = vendorRepository.findById(id);
        if (vendorOpt.isPresent()) {
            Vendor vendor = vendorOpt.get();
            vendorRepository.delete(id);
            userService.deleteUser(vendor.getUserId());
            logger.info("Vendor deleted: {}", id);
        }
    }

    public static class VendorRegistrationResult {
        private final boolean success;
        private final Vendor vendor;
        private final String message;

        private VendorRegistrationResult(boolean success, Vendor vendor, String message) {
            this.success = success;
            this.vendor = vendor;
            this.message = message;
        }

        public static VendorRegistrationResult success(Vendor vendor) {
            return new VendorRegistrationResult(true, vendor, null);
        }

        public static VendorRegistrationResult failure(String message) {
            return new VendorRegistrationResult(false, null, message);
        }

        public boolean isSuccess() { return success; }
        public Vendor getVendor() { return vendor; }
        public String getMessage() { return message; }
    }
}
