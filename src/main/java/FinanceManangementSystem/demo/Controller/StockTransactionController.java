package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Enums.StockTransactionType;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestStockTransactionDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseStockTransactionDTO;
import FinanceManangementSystem.demo.Service.StockTransactionServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("stock-transaction")
@RequiredArgsConstructor
public class StockTransactionController {

    private final StockTransactionServiceInterface
            stockTransactionService;


    // =========================================================
    // CREATE STOCK ADJUSTMENT
    // =========================================================

    @PostMapping("adjustment")
    public ResponseEntity<
            APIResponse<ResponseStockTransactionDTO>
            >
    createAdjustment(
            @Valid @RequestBody
            RequestStockTransactionDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in createAdjustment..."
        );

        log.info(
                "CONTROLLER - calling stock transaction service..."
        );

        ResponseStockTransactionDTO response =
                stockTransactionService
                        .createAdjustment(dto);

        log.info(
                "CONTROLLER - stock adjustment created successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new APIResponse<>(
                                "Stock adjustment created successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET TRANSACTION BY PUBLIC ID
    // =========================================================

    @GetMapping("/{publicId}")
    public ResponseEntity<
            APIResponse<ResponseStockTransactionDTO>
            >
    getTransactionByPublicId(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in getTransactionByPublicId..."
        );

        log.info(
                "CONTROLLER - calling stock transaction service..."
        );

        ResponseStockTransactionDTO response =
                stockTransactionService
                        .getTransactionByPublicId(publicId);

        log.info(
                "CONTROLLER - stock transaction fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock transaction fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET ALL TRANSACTIONS
    // =========================================================

    @GetMapping("all")
    public ResponseEntity<
            APIResponse<org.springframework.data.domain.Page<ResponseStockTransactionDTO>>
            >
    getAllTransactions(
            @RequestParam(required = false) UUID stockPublicId,
            @RequestParam(required = false) StockTransactionType type,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("CONTROLLER - request came in getAllTransactions with filters...");
        var pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "transactionDate"));

        org.springframework.data.domain.Page<ResponseStockTransactionDTO> response =
                stockTransactionService.getAllTransactions(stockPublicId, type, referenceNumber, fromDate, toDate, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "All stock transactions fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET TRANSACTIONS BY STOCK
    // =========================================================

    @GetMapping("stock/{stockPublicId}")
    public ResponseEntity<
            APIResponse<List<ResponseStockTransactionDTO>>
            >
    getTransactionsByStockPublicId(
            @PathVariable UUID stockPublicId
    ) {

        log.info(
                "CONTROLLER - request came in getTransactionsByStockPublicId..."
        );

        log.info(
                "CONTROLLER - calling stock transaction service..."
        );

        List<ResponseStockTransactionDTO> response =
                stockTransactionService
                        .getTransactionsByStockPublicId(
                                stockPublicId
                        );

        log.info(
                "CONTROLLER - stock transactions fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock transactions fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET TRANSACTIONS BY STOCK AND TYPE
    // =========================================================

    @GetMapping(
            "stock/{stockPublicId}/type/{transactionType}"
    )
    public ResponseEntity<
            APIResponse<List<ResponseStockTransactionDTO>>
            >
    getTransactionsByType(
            @PathVariable UUID stockPublicId,
            @PathVariable StockTransactionType transactionType
    ) {

        log.info(
                "CONTROLLER - request came in getTransactionsByType..."
        );

        log.info(
                "CONTROLLER - calling stock transaction service..."
        );

        List<ResponseStockTransactionDTO> response =
                stockTransactionService
                        .getTransactionsByType(
                                stockPublicId,
                                transactionType
                        );

        log.info(
                "CONTROLLER - stock transactions filtered successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock transactions filtered successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET TRANSACTIONS BETWEEN DATES
    // =========================================================

    @GetMapping("date-range")
    public ResponseEntity<
            APIResponse<List<ResponseStockTransactionDTO>>
            >
    getTransactionsBetweenDates(
            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime fromDate,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime toDate
    ) {

        log.info(
                "CONTROLLER - request came in getTransactionsBetweenDates..."
        );

        log.info(
                "CONTROLLER - calling stock transaction service..."
        );

        List<ResponseStockTransactionDTO> response =
                stockTransactionService
                        .getTransactionsBetweenDates(
                                fromDate,
                                toDate
                        );

        log.info(
                "CONTROLLER - stock transactions fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock transactions fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET TRANSACTIONS BY TRANSACTION TYPE
    // =========================================================

    @GetMapping("type/{transactionType}")
    public ResponseEntity<
            APIResponse<List<ResponseStockTransactionDTO>>
            >
    getTransactionsByTransactionType(
            @PathVariable StockTransactionType transactionType
    ) {

        log.info(
                "CONTROLLER - request came in getTransactionsByTransactionType..."
        );

        log.info(
                "CONTROLLER - calling stock transaction service..."
        );

        List<ResponseStockTransactionDTO> response =
                stockTransactionService
                        .getTransactionsByTransactionType(
                                transactionType
                        );

        log.info(
                "CONTROLLER - stock transactions fetched by type successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock transactions fetched by type successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET TRANSACTIONS BY REFERENCE NUMBER
    // =========================================================

    @GetMapping("reference/{referenceNumber}")
    public ResponseEntity<
            APIResponse<List<ResponseStockTransactionDTO>>
            >
    getTransactionsByReferenceNumber(
            @PathVariable String referenceNumber
    ) {

        log.info(
                "CONTROLLER - request came in getTransactionsByReferenceNumber..."
        );

        log.info(
                "CONTROLLER - calling stock transaction service..."
        );

        List<ResponseStockTransactionDTO> response =
                stockTransactionService
                        .getTransactionsByReferenceNumber(
                                referenceNumber
                        );

        log.info(
                "CONTROLLER - stock transactions fetched by reference number successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock transactions fetched by reference number successfully...",
                                response
                        )
                );
    }
}