package com.gtplug.services;

import com.gtplug.models.AdminLog;
import com.gtplug.repositories.AdminLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminLogService {
    private static final Logger logger = LoggerFactory.getLogger(AdminLogService.class);
    private final AdminLogRepository adminLogRepository;

    public AdminLogService() {
        this.adminLogRepository = new AdminLogRepository();
    }

    public AdminLog logAction(Long adminId, String action, Long projectId, Long vendorId, String details) {
        AdminLog log = new AdminLog(adminId, action, projectId, vendorId, details);
        return adminLogRepository.save(log);
    }

    public AdminLog logAction(Long adminId, String action, String details) {
        AdminLog log = new AdminLog(adminId, action, details);
        return adminLogRepository.save(log);
    }

    public List<AdminLog> getAllLogs(int page, int size) {
        return adminLogRepository.findAll(page, size);
    }

    public List<AdminLog> getLogsByAdmin(Long adminId, int page, int size) {
        return adminLogRepository.findByAdminId(adminId, page, size);
    }

    public long count() {
        return adminLogRepository.count();
    }
}
