package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestInvestmentDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseInvestmentDTO;
import FinanceManangementSystem.demo.Service.InvestmentServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/investment")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentServiceInterface investmentService;

    @PostMapping("/add")
    public ResponseEntity<APIResponse<ResponseInvestmentDTO>> addInvestment(
            @Valid @RequestBody RequestInvestmentDTO dto
    ) {
        log.info("CONTROLLER - request came in addInvestment...");
        ResponseInvestmentDTO resp = investmentService.addInvestment(dto);
        return ResponseEntity.ok(new APIResponse<>("Investment recorded successfully", resp));
    }

    @GetMapping("/all")
    public ResponseEntity<APIResponse<List<ResponseInvestmentDTO>>> getAllInvestments() {
        log.info("CONTROLLER - request came in getAllInvestments...");
        List<ResponseInvestmentDTO> list = investmentService.getAllInvestments();
        return ResponseEntity.ok(new APIResponse<>("All investments fetched successfully", list));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseInvestmentDTO>> getInvestment(
            @PathVariable UUID publicId
    ) {
        log.info("CONTROLLER - request came in getInvestment for publicId: {}", publicId);
        ResponseInvestmentDTO resp = investmentService.getInvestmentByPublicId(publicId);
        return ResponseEntity.ok(new APIResponse<>("Investment fetched successfully", resp));
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseInvestmentDTO>> updateInvestment(
            @PathVariable UUID publicId,
            @Valid @RequestBody RequestInvestmentDTO dto
    ) {
        log.info("CONTROLLER - request came in updateInvestment for publicId: {}", publicId);
        ResponseInvestmentDTO resp = investmentService.updateInvestment(publicId, dto);
        return ResponseEntity.ok(new APIResponse<>("Investment updated successfully", resp));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<APIResponse<Void>> deleteInvestment(
            @PathVariable UUID publicId
    ) {
        log.info("CONTROLLER - request came in deleteInvestment for publicId: {}", publicId);
        investmentService.deleteInvestment(publicId);
        return ResponseEntity.ok(new APIResponse<>("Investment deleted successfully", null));
    }

    @GetMapping("/total")
    public ResponseEntity<APIResponse<BigDecimal>> getTotalInvestments() {
        log.info("CONTROLLER - request came in getTotalInvestments...");
        BigDecimal total = investmentService.getTotalInvestments();
        return ResponseEntity.ok(new APIResponse<>("Total investment fetched successfully", total));
    }
}
