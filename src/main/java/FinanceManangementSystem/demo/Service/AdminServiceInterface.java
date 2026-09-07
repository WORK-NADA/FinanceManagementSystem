package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUpdateUserDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUserDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseUserDTO;

import FinanceManangementSystem.demo.Payloads.ResponseDTO.AdminDashboardStatsDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.Client360DTO;

import java.util.List;
import java.util.UUID;

public interface AdminServiceInterface {
    ResponseUserDTO registration(RequestUserDTO dto);
    List<ResponseUserDTO> listAllUsers();
    ResponseUserDTO getUserByPublicId(UUID publicId);
    ResponseUserDTO updateUser(UUID publicId, RequestUpdateUserDTO dto);
    void deactivateUser(UUID publicId);
    void reactivateUser(UUID publicId);
    AdminDashboardStatsDTO getAdminDashboardStats();
    Client360DTO getClient360(UUID publicId);
    void unlockUser(UUID publicId);
}
