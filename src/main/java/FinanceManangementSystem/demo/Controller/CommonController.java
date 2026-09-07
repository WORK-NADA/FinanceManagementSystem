package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestLoginDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUpdateUserDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseLoginDTO;
import FinanceManangementSystem.demo.Service.Implementations.CommonService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseUserDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("user")
public class CommonController {

    @Autowired
    CommonService service;

    @PostMapping("login")
    public ResponseEntity<APIResponse<ResponseLoginDTO>> login(@Valid @RequestBody RequestLoginDTO dto){
        log.info("CONTROLLER - request came in login controller...");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>("Logged in successfully...",
                                service.login(dto))
                );
    }

    @GetMapping("me")
    public ResponseEntity<APIResponse<ResponseUserDTO>> getCurrentUser() {
        log.info("CONTROLLER - request came in getCurrentUser...");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>("Current user fetched successfully...",
                                service.getCurrentUser())
                );
    }

    @PutMapping("me")
    public ResponseEntity<APIResponse<ResponseUserDTO>> updateCurrentUser(@Valid @RequestBody RequestUpdateUserDTO dto) {
        log.info("CONTROLLER - request came in updateCurrentUser...");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>("Profile updated successfully...",
                                service.updateCurrentUser(dto))
                );
    }
}
