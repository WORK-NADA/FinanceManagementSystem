package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUpdateUserDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUserDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseUserDTO;
import FinanceManangementSystem.demo.Service.Implementations.AdminService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("admin")
public class AdminController {

    @Autowired
    AdminService service;

    @PostMapping("register")
    public ResponseEntity<APIResponse<ResponseUserDTO>> register(@Valid @RequestBody RequestUserDTO dto){
        log.info("CONTROLLER - request came in register controller...");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>("Registered successfully...",
                                service.registration(dto))
                );
    }

    @GetMapping("users")
    public ResponseEntity<APIResponse<List<ResponseUserDTO>>> listUsers() {
        log.info("CONTROLLER - request came in listUsers...");
        return ResponseEntity.ok(new APIResponse<>("Users fetched successfully...", service.listAllUsers()));
    }

    @GetMapping("users/{publicId}")
    public ResponseEntity<APIResponse<ResponseUserDTO>> getUserByPublicId(@PathVariable UUID publicId) {
        log.info("CONTROLLER - request came in getUserByPublicId for publicId: {}", publicId);
        return ResponseEntity.ok(
                new APIResponse<>("Client fetched successfully...", service.getUserByPublicId(publicId))
        );
    }

    @PutMapping("users/{publicId}")
    public ResponseEntity<APIResponse<ResponseUserDTO>> updateUser(
            @PathVariable UUID publicId,
            @Valid @RequestBody RequestUpdateUserDTO dto
    ) {
        log.info("CONTROLLER - request came in updateUser for publicId: {}", publicId);
        return ResponseEntity.ok(
                new APIResponse<>("Client updated successfully...", service.updateUser(publicId, dto))
        );
    }

    @PatchMapping("users/{publicId}/deactivate")
    public ResponseEntity<APIResponse<Void>> deactivateUser(@PathVariable UUID publicId) {
        log.info("CONTROLLER - request came in deactivateUser for publicId: {}", publicId);
        service.deactivateUser(publicId);
        return ResponseEntity.ok(new APIResponse<>("Client deactivated successfully...", null));
    }

    @PatchMapping("users/{publicId}/reactivate")
    public ResponseEntity<APIResponse<Void>> reactivateUser(@PathVariable UUID publicId) {
        log.info("CONTROLLER - request came in reactivateUser for publicId: {}", publicId);
        service.reactivateUser(publicId);
        return ResponseEntity.ok(new APIResponse<>("Client reactivated successfully...", null));
    }
}
