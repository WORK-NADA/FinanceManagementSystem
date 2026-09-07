package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestSaleDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseSaleDTO;
import FinanceManangementSystem.demo.Service.SaleServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import FinanceManangementSystem.demo.Enums.PaymentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("sale")
@RequiredArgsConstructor
public class SaleController {

    private final SaleServiceInterface saleService;


    // =========================================================
    // ADD SALE
    // =========================================================

    @PostMapping("add")
    public ResponseEntity<APIResponse<ResponseSaleDTO>>
    addSale(
            @Valid @RequestBody RequestSaleDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in addSale..."
        );


        log.info(
                "CONTROLLER - calling sale service..."
        );

        ResponseSaleDTO response =
                saleService.addSale(
                        dto
                );


        log.info(
                "CONTROLLER - sale created successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new APIResponse<>(
                                "Sale created successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET SALE BY PUBLIC ID
    // =========================================================

    @GetMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseSaleDTO>>
    getSaleByPublicId(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in getSaleByPublicId..."
        );


        log.info(
                "CONTROLLER - calling sale service..."
        );

        ResponseSaleDTO response =
                saleService.getSaleByPublicId(
                        publicId
                );


        log.info(
                "CONTROLLER - sale fetched successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Sale fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET ALL SALES
    // =========================================================

    @GetMapping("all")
    public ResponseEntity<APIResponse<Page<ResponseSaleDTO>>>
    getAllSales(
            @RequestParam(required = false) UUID customerPublicId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("CONTROLLER - request came in getAllSales with filters...");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "saleDate"));

        Page<ResponseSaleDTO> response = saleService.getAllSales(customerPublicId, status, fromDate, toDate, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "All sales fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // UPDATE SALE
    // =========================================================

    @PutMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseSaleDTO>>
    updateSale(
            @PathVariable UUID publicId,
            @Valid @RequestBody RequestSaleDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in updateSale..."
        );


        log.info(
                "CONTROLLER - calling sale service..."
        );

        ResponseSaleDTO response =
                saleService.updateSale(
                        publicId,
                        dto
                );


        log.info(
                "CONTROLLER - sale updated successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Sale updated successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET SALES BY CUSTOMER
    // =========================================================

    @GetMapping("customer/{customerPublicId}")
    public ResponseEntity<
            APIResponse<List<ResponseSaleDTO>>
            >
    getSalesByCustomer(
            @PathVariable UUID customerPublicId
    ) {

        log.info(
                "CONTROLLER - request came in getSalesByCustomer..."
        );


        log.info(
                "CONTROLLER - calling sale service..."
        );

        List<ResponseSaleDTO> response =
                saleService.getSalesByCustomer(
                        customerPublicId
                );


        log.info(
                "CONTROLLER - customer sales fetched successfully..."
        );


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Customer sales fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // DELETE SALE
    // =========================================================

    @DeleteMapping("/{publicId}")
    public ResponseEntity<APIResponse<Void>> deleteSale(
            @PathVariable UUID publicId
    ) {
        log.info("CONTROLLER - request came in deleteSale: {}", publicId);
        saleService.deleteSale(publicId);
        log.info("CONTROLLER - sale deleted successfully: {}", publicId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Sale deleted successfully...",
                                null
                        )
                );
    }
}
