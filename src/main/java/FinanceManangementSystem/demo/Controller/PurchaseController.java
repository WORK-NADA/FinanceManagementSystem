package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestPurchaseDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePurchaseDTO;
import FinanceManangementSystem.demo.Service.PurchaseServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.data.domain.Pageable;
import FinanceManangementSystem.demo.Enums.PaymentStatus;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseServiceInterface purchaseService;


    // =========================================================
    // ADD PURCHASE
    // =========================================================

    @PostMapping("add")
    public ResponseEntity<APIResponse<ResponsePurchaseDTO>> addPurchase(
            @Valid @RequestBody RequestPurchaseDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in addPurchase..."
        );


        log.info(
                "CONTROLLER - calling purchase service..."
        );

        ResponsePurchaseDTO response =
                purchaseService.addPurchase(dto);


        log.info(
                "CONTROLLER - purchase added successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new APIResponse<>(
                                "Purchase added successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET PURCHASE BY PUBLIC ID
    // =========================================================

    @GetMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponsePurchaseDTO>>
    getPurchaseByPublicId(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in getPurchaseByPublicId..."
        );


        log.info(
                "CONTROLLER - calling purchase service..."
        );

        ResponsePurchaseDTO response =
                purchaseService.getPurchaseByPublicId(
                        publicId
                );


        log.info(
                "CONTROLLER - purchase fetched successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Purchase fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET ALL PURCHASES
    // =========================================================

    @GetMapping("all")
    public ResponseEntity<
            APIResponse<Page<ResponsePurchaseDTO>>
            >
    getAllPurchases(
            @RequestParam(required = false) UUID supplierPublicId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("CONTROLLER - request came in getAllPurchases with filters...");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "purchaseDate"));

        Page<ResponsePurchaseDTO> response = purchaseService.getAllPurchases(supplierPublicId, status, fromDate, toDate, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Purchases fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET PURCHASES BY SUPPLIER
    // =========================================================

    @GetMapping("supplier/{supplierPublicId}")
    public ResponseEntity<
            APIResponse<List<ResponsePurchaseDTO>>
            >
    getPurchasesBySupplier(
            @PathVariable UUID supplierPublicId
    ) {

        log.info(
                "CONTROLLER - request came in getPurchasesBySupplier..."
        );


        log.info(
                "CONTROLLER - calling purchase service..."
        );

        List<ResponsePurchaseDTO> response =
                purchaseService.getPurchasesBySupplier(
                        supplierPublicId
                );


        log.info(
                "CONTROLLER - supplier purchases fetched successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Supplier purchases fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET PURCHASES BY DATE RANGE
    // =========================================================

    @GetMapping("date-range")
    public ResponseEntity<
            APIResponse<List<ResponsePurchaseDTO>>
            >
    getPurchasesByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {

        log.info(
                "CONTROLLER - request came in getPurchasesByDateRange..."
        );


        log.info(
                "CONTROLLER - calling purchase service..."
        );

        List<ResponsePurchaseDTO> response =
                purchaseService.getPurchasesByDateRange(
                        fromDate,
                        toDate
                );


        log.info(
                "CONTROLLER - purchases fetched successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Purchases fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET PURCHASES BY STATUS
    // =========================================================

    @GetMapping("status/{status}")
    public ResponseEntity<
            APIResponse<List<ResponsePurchaseDTO>>
            >
    getPurchasesByStatus(
            @PathVariable String status
    ) {

        log.info(
                "CONTROLLER - request came in getPurchasesByStatus..."
        );


        log.info(
                "CONTROLLER - calling purchase service..."
        );

        List<ResponsePurchaseDTO> response =
                purchaseService.getPurchasesByStatus(
                        status
                );


        log.info(
                "CONTROLLER - purchases fetched by status successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Purchases fetched by status successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // UPDATE PURCHASE
    // =========================================================

    @PutMapping("/{publicId}")
    public ResponseEntity<
            APIResponse<ResponsePurchaseDTO>
            >
    updatePurchase(
            @PathVariable UUID publicId,

            @Valid @RequestBody RequestPurchaseDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in updatePurchase..."
        );


        log.info(
                "CONTROLLER - calling purchase service..."
        );

        ResponsePurchaseDTO response =
                purchaseService.updatePurchase(
                        publicId,
                        dto
                );


        log.info(
                "CONTROLLER - purchase updated successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Purchase updated successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // DELETE PURCHASE
    // =========================================================

    @DeleteMapping("/{publicId}")
    public ResponseEntity<APIResponse<Void>> deletePurchase(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in deletePurchase: {}",
                publicId
        );

        purchaseService.deletePurchase(publicId);

        log.info(
                "CONTROLLER - purchase deleted successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Purchase deleted successfully...",
                                null
                        )
                );
    }
}