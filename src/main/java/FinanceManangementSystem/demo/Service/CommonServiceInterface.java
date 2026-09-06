package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestLoginDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUpdateUserDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseLoginDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseUserDTO;

public interface CommonServiceInterface {
    ResponseLoginDTO login(RequestLoginDTO dto);
    ResponseUserDTO getCurrentUser();
    ResponseUserDTO updateCurrentUser(RequestUpdateUserDTO dto);
}
