package FinanceManangementSystem.demo.Controller;

import FinanceManangementSystem.demo.APIResponse.APIResponse;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestMinimumStockLevelDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestStockDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseStockDTO;
import FinanceManangementSystem.demo.Service.StockServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("stock")
@RequiredArgsConstructor
public class StockController {

    private final StockServiceInterface stockService;


    // =========================================================
    // ADD STOCK
    // =========================================================

    @PostMapping("add")
    public ResponseEntity<APIResponse<ResponseStockDTO>>
    addStock(
            @Valid @RequestBody RequestStockDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in addStock..."
        );

        log.info(
                "CONTROLLER - calling stock service..."
        );

        ResponseStockDTO response =
                stockService.addStock(dto);

        log.info(
                "CONTROLLER - stock added successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new APIResponse<>(
                                "Stock added successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET STOCK BY PUBLIC ID
    // =========================================================

    @GetMapping("/{publicId}")
    public ResponseEntity<APIResponse<ResponseStockDTO>>
    getStockByPublicId(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in getStockByPublicId..."
        );

        log.info(
                "CONTROLLER - calling stock service..."
        );

        ResponseStockDTO response =
                stockService.getStockByPublicId(publicId);

        log.info(
                "CONTROLLER - stock fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET ALL STOCKS
    // =========================================================

    @GetMapping("all")
    public ResponseEntity<
            APIResponse<List<ResponseStockDTO>>
            >
    getAllStocks() {

        log.info(
                "CONTROLLER - request came in getAllStocks..."
        );

        log.info(
                "CONTROLLER - calling stock service..."
        );

        List<ResponseStockDTO> response =
                stockService.getAllStocks();

        log.info(
                "CONTROLLER - all stocks fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "All stocks fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET ALL ACTIVE STOCKS
    // =========================================================

    @GetMapping("active")
    public ResponseEntity<
            APIResponse<List<ResponseStockDTO>>
            >
    getAllActiveStocks() {

        log.info(
                "CONTROLLER - request came in getAllActiveStocks..."
        );

        log.info(
                "CONTROLLER - calling stock service..."
        );

        List<ResponseStockDTO> response =
                stockService.getAllActiveStocks();

        log.info(
                "CONTROLLER - active stocks fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Active stocks fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // GET ALL INACTIVE STOCKS
    // =========================================================

    @GetMapping("inactive")
    public ResponseEntity<
            APIResponse<List<ResponseStockDTO>>
            >
    getAllInactiveStocks() {

        log.info(
                "CONTROLLER - request came in getAllInactiveStocks..."
        );

        log.info(
                "CONTROLLER - calling stock service..."
        );

        List<ResponseStockDTO> response =
                stockService.getAllInactiveStocks();

        log.info(
                "CONTROLLER - inactive stocks fetched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Inactive stocks fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // SEARCH ACTIVE STOCK
    // =========================================================

    @GetMapping("search")
    public ResponseEntity<
            APIResponse<List<ResponseStockDTO>>
            >
    searchStock(
            @RequestParam(required = false) String rawMaterial,
            @RequestParam(required = false) String query
    ) {

        log.info(
                "CONTROLLER - request came in searchStock..."
        );

        String searchTerm = (query != null && !query.isBlank())
                ? query
                : (rawMaterial != null ? rawMaterial : "");

        log.info(
                "CONTROLLER - calling stock service with searchTerm: {}...",
                searchTerm
        );

        List<ResponseStockDTO> response =
                stockService.searchStock(searchTerm);

        log.info(
                "CONTROLLER - stocks searched successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stocks fetched successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // UPDATE STOCK MASTER DETAILS
    // =========================================================

    @PutMapping("/{publicId}")
    public ResponseEntity<
            APIResponse<ResponseStockDTO>
            >
    updateStock(
            @PathVariable UUID publicId,
            @Valid @RequestBody RequestStockDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in updateStock..."
        );

        log.info(
                "CONTROLLER - calling stock service..."
        );

        ResponseStockDTO response =
                stockService.updateStock(
                        publicId,
                        dto
                );

        log.info(
                "CONTROLLER - stock updated successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock updated successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // UPDATE MINIMUM STOCK LEVEL
    // =========================================================

    @PutMapping("/{publicId}/minimum-level")
    public ResponseEntity<
            APIResponse<ResponseStockDTO>
            >
    updateMinimumStockLevel(
            @PathVariable UUID publicId,
            @Valid @RequestBody
            RequestMinimumStockLevelDTO dto
    ) {

        log.info(
                "CONTROLLER - request came in updateMinimumStockLevel..."
        );

        log.info(
                "CONTROLLER - calling stock service..."
        );

        ResponseStockDTO response =
                stockService.updateMinimumStockLevel(
                        publicId,
                        dto
                );

        log.info(
                "CONTROLLER - minimum stock level updated successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Minimum stock level updated successfully...",
                                response
                        )
                );
    }


    // =========================================================
    // DEACTIVATE STOCK
    // =========================================================

    @PatchMapping("/{publicId}/deactivate")
    public ResponseEntity<APIResponse<Void>>
    deactivateStock(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in deactivateStock..."
        );

        log.info(
                "CONTROLLER - calling stock service..."
        );

        stockService.deactivateStock(publicId);

        log.info(
                "CONTROLLER - stock deactivated successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock deactivated successfully...",
                                null
                        )
                );
    }


    // =========================================================
    // ACTIVATE STOCK
    // =========================================================

    @PatchMapping("/{publicId}/activate")
    public ResponseEntity<APIResponse<Void>>
    activateStock(
            @PathVariable UUID publicId
    ) {

        log.info(
                "CONTROLLER - request came in activateStock..."
        );

        log.info(
                "CONTROLLER - calling stock service..."
        );

        stockService.activateStock(publicId);

        log.info(
                "CONTROLLER - stock activated successfully..."
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new APIResponse<>(
                                "Stock activated successfully...",
                                null
                        )
                );
    }
}