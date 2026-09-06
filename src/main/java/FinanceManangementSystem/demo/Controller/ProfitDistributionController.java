package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestProfitDistributionDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestProfitWithdrawalDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.LiveProfitSharingOverviewDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseProfitDistributionDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseProfitWithdrawalDTO;
import FinanceManangementSystem.demo.Service.ProfitDistributionServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/profit-distribution")
@RequiredArgsConstructor
public class ProfitDistributionController {

    private final ProfitDistributionServiceInterface distributionService;

    @PostMapping("/preview")
    public ResponseEntity<APIResponse<ResponseProfitDistributionDTO>> preview(
            @Valid @RequestBody RequestProfitDistributionDTO dto
    ) {
        log.info("CONTROLLER - request came in preview distribution...");

        ResponseProfitDistributionDTO resp = distributionService.previewDistribution(dto);

        return ResponseEntity.ok(new APIResponse<>("Distribution preview generated", resp));
    }

    @PostMapping("/distribute")
    public ResponseEntity<APIResponse<ResponseProfitDistributionDTO>> distribute(
            @Valid @RequestBody RequestProfitDistributionDTO dto
    ) {
        log.info("CONTROLLER - request came in distribute...");

        ResponseProfitDistributionDTO resp = distributionService.calculateAndDistribute(dto);

        return ResponseEntity.ok(new APIResponse<>("Distribution completed", resp));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseProfitDistributionDTO>> getById(@PathVariable UUID publicId) {
        log.info("CONTROLLER - request came in getDistributionByPublicId...");

        ResponseProfitDistributionDTO resp = distributionService.getDistributionByPublicId(publicId);

        return ResponseEntity.ok(new APIResponse<>("Distribution fetched", resp));
    }

    @GetMapping("/all")
    public ResponseEntity<APIResponse<List<ResponseProfitDistributionDTO>>> getAll() {
        log.info("CONTROLLER - request came in getAllDistributions...");

        List<ResponseProfitDistributionDTO> list = distributionService.getAllDistributions();

        return ResponseEntity.ok(new APIResponse<>("All distributions fetched", list));
    }

    @GetMapping("/partner/{partnerPublicId}/history")
    public ResponseEntity<APIResponse<List<ResponseProfitDistributionDTO.PartnerShareDetails>>> getShareHistory(
            @PathVariable UUID partnerPublicId
    ) {
        log.info("CONTROLLER - request came in getShareHistoryByPartner...");

        List<ResponseProfitDistributionDTO.PartnerShareDetails> list = distributionService.getShareHistoryByPartner(partnerPublicId);

        return ResponseEntity.ok(new APIResponse<>("Share history fetched", list));
    }

    @GetMapping("/latest")
    public ResponseEntity<APIResponse<ResponseProfitDistributionDTO>> getLatest() {
        log.info("CONTROLLER - request came in getLatestDistribution...");

        ResponseProfitDistributionDTO resp = distributionService.getLatestDistribution();

        return ResponseEntity.ok(new APIResponse<>("Latest distribution fetched", resp));
    }

    @GetMapping("/partner/{partnerPublicId}/lifetime-earnings")
    public ResponseEntity<APIResponse<BigDecimal>> getLifetimeEarnings(@PathVariable UUID partnerPublicId) {
        log.info("CONTROLLER - request came in getLifetimeEarningsByPartner...");

        BigDecimal sum = distributionService.getLifetimeEarningsByPartner(partnerPublicId);

        return ResponseEntity.ok(new APIResponse<>("Lifetime earnings fetched", sum));
    }

    @GetMapping("/live-overview")
    public ResponseEntity<APIResponse<LiveProfitSharingOverviewDTO>> getLiveOverview() {
        log.info("CONTROLLER - request came in getLiveOverview...");

        LiveProfitSharingOverviewDTO overview = distributionService.getLiveProfitOverview();

        return ResponseEntity.ok(new APIResponse<>("Live profit overview fetched", overview));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<APIResponse<ResponseProfitWithdrawalDTO>> withdraw(
            @Valid @RequestBody RequestProfitWithdrawalDTO dto
    ) {
        log.info("CONTROLLER - request came in withdraw...");

        ResponseProfitWithdrawalDTO resp = distributionService.recordWithdrawal(dto);

        return ResponseEntity.ok(new APIResponse<>("Profit withdrawal recorded successfully", resp));
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<APIResponse<List<ResponseProfitWithdrawalDTO>>> getWithdrawals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        log.info("CONTROLLER - request came in getWithdrawals... fromDate={}, toDate={}", fromDate, toDate);

        List<ResponseProfitWithdrawalDTO> list = distributionService.getWithdrawalHistory(fromDate, toDate);

        return ResponseEntity.ok(new APIResponse<>("Withdrawal history fetched", list));
    }
}
