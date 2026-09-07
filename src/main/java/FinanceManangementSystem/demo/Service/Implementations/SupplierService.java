package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;


import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Exceptions.DuplicateResourceException;
import FinanceManangementSystem.demo.Exceptions.InvalidStateException;
import FinanceManangementSystem.demo.Model.Supplier;
import FinanceManangementSystem.demo.Model.SupplierAddress;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestSupplierDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseSupplierDTO;
import FinanceManangementSystem.demo.Repository.SupplierRepository;
import FinanceManangementSystem.demo.Service.SupplierServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import FinanceManangementSystem.demo.Model.Purchase;
import FinanceManangementSystem.demo.Model.PurchasePayment;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePartyStatementDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePartyStatementDTO.LedgerEntryDTO;
import FinanceManangementSystem.demo.Repository.PurchaseRepository;
import FinanceManangementSystem.demo.Repository.PurchasePaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService implements SupplierServiceInterface {

    private final SupplierRepository supplierRepo;

    private final CurrentUserService currentUserService;

    private final ModelMapper modelMapper;

    private final PurchaseRepository purchaseRepo;

    private final PurchasePaymentRepository purchasePaymentRepo;


    // ==================================================
    // ADD SUPPLIER
    // ==================================================

    @Override
    @Transactional
    public ResponseSupplierDTO addSupplier(
            RequestSupplierDTO dto
    ) {

        log.info(
                "SERVICE - request came in addSupplier..."
        );


        // ----------------------------------------------
        // Check Mobile Number
        // ----------------------------------------------

        log.info(
                "SERVICE - checking supplier mobile number..."
        );

        User currentUser = currentUserService.getCurrentUser();

        if (supplierRepo.existsByUserAndMobileNumber(
                currentUser,
                dto.getMobileNumber()
        )) {

            log.info(
                    "SERVICE - supplier mobile number already exists..."
            );

            throw new DuplicateResourceException(
                    "Supplier with this mobile number already exists"
            );
        }


        // ----------------------------------------------
        // Check GST Number
        // ----------------------------------------------

        if (dto.getGstNumber() != null
                && !dto.getGstNumber().isBlank()) {
            String trimmedGst = dto.getGstNumber().trim().toUpperCase();
            if (supplierRepo.existsByUserAndGstNumber(
                    currentUser,
                    trimmedGst
            )) {

                log.info(
                        "SERVICE - supplier GST number already exists..."
                );

                throw new DuplicateResourceException(
                        "Supplier with this GST number already exists"
                );
            }
        }


        // ----------------------------------------------
        // Map DTO → Entity
        // ----------------------------------------------

        log.info(
                "SERVICE - mapping supplier DTO to entity..."
        );

        Supplier supplier =
                modelMapper.map(
                        dto,
                        Supplier.class
                );

        supplier.setUser(currentUser);

        // Sanitize optional fields to null if blank
        if (supplier.getAlternateMobileNumber() != null) {
            String trimmed = supplier.getAlternateMobileNumber().trim();
            supplier.setAlternateMobileNumber(trimmed.isBlank() ? null : trimmed);
        }
        if (supplier.getGstNumber() != null) {
            String trimmed = supplier.getGstNumber().trim().toUpperCase();
            supplier.setGstNumber(trimmed.isBlank() ? null : trimmed);
        }
        if (supplier.getEmail() != null) {
            String trimmed = supplier.getEmail().trim();
            supplier.setEmail(trimmed.isBlank() ? null : trimmed);
        }
        if (supplier.getContactPerson() != null) {
            String trimmed = supplier.getContactPerson().trim();
            supplier.setContactPerson(trimmed.isBlank() ? null : trimmed);
        }


        // ----------------------------------------------
        // Explicitly Handle Supplier Address
        // ----------------------------------------------

        if (dto.getAddress() != null
                && dto.getAddress().getAddressLine1() != null
                && !dto.getAddress().getAddressLine1().isBlank()) {

            log.info(
                    "SERVICE - supplier address found..."
            );

            SupplierAddress address =
                    modelMapper.map(
                            dto.getAddress(),
                            SupplierAddress.class
                    );

            if (address.getCity() == null || address.getCity().isBlank()) {
                address.setCity("Unknown");
            }
            if (address.getState() == null || address.getState().isBlank()) {
                address.setState("Unknown");
            }
            if (address.getCountry() == null || address.getCountry().isBlank()) {
                address.setCountry("India");
            }
            if (address.getPincode() == null || address.getPincode().isBlank()) {
                address.setPincode("000000");
            }

            // Explicit relationship
            address.setSupplier(supplier);

            supplier.setAddress(address);
        } else {
            supplier.setAddress(null);
        }


        // ----------------------------------------------
        // Set Default Values
        // ----------------------------------------------

        if (supplier.getOpeningBalance() == null) {

            supplier.setOpeningBalance(
                    BigDecimal.ZERO
            );
        }

        if (supplier.getPaymentTerms() == null) {

            supplier.setPaymentTerms(30);
        }

        if (supplier.getIsActive() == null) {

            supplier.setIsActive(true);
        }


        // ----------------------------------------------
        // Save Supplier
        // ----------------------------------------------

        log.info(
                "SERVICE - saving supplier..."
        );

        supplier =
                supplierRepo.save(supplier);


        log.info(
                "SERVICE - supplier added successfully..."
        );


        // ----------------------------------------------
        // Map Entity → Response
        // ----------------------------------------------

        return mapToResponse(supplier);
    }


    // ==================================================
    // GET SUPPLIER BY PUBLIC ID
    // ==================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseSupplierDTO getSupplierByPublicId(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in getSupplierByPublicId..."
        );

        User currentUser = currentUserService.getCurrentUser();
        Supplier supplier;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            supplier = supplierRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        } else {
            supplier = supplierRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        }


        log.info(
                "SERVICE - supplier fetched successfully..."
        );


        return mapToResponse(supplier);
    }


    // ==================================================
    // GET ALL SUPPLIERS
    // ==================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseSupplierDTO> getAllSuppliers() {

        log.info(
                "SERVICE - request came in getAllSuppliers..."
        );


        User currentUser = currentUserService.getCurrentUser();

        List<Supplier> suppliers =
                currentUser.getRole() == UserRole.ADMIN
                        ? supplierRepo.findAll()
                        : supplierRepo.findByUser(currentUser);


        log.info(
                "SERVICE - suppliers fetched successfully..."
        );


        return suppliers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================================================
    // GET ALL ACTIVE SUPPLIERS
    // ==================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseSupplierDTO> getAllActiveSuppliers() {

        log.info(
                "SERVICE - request came in getAllActiveSuppliers..."
        );

        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return supplierRepo.findByIsActiveTrue().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        return supplierRepo
                .findByUserAndIsActiveTrue(currentUser)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // ==================================================
    // UPDATE SUPPLIER
    // ==================================================

    @Override
    @Transactional
    public ResponseSupplierDTO updateSupplier(
            UUID publicId,
            RequestSupplierDTO dto
    ) {

        log.info(
                "SERVICE - request came in updateSupplier..."
        );


        // ----------------------------------------------
        // Find Supplier
        // ----------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        Supplier supplier;

        if (currentUser.getRole() == UserRole.ADMIN) {
            supplier = supplierRepo.findByPublicId(publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - supplier not found...");
                        return new ResourceNotFoundException("Supplier not found");
                    });
        } else {
            supplier = supplierRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - supplier not found for current user...");
                        return new ResourceNotFoundException("Supplier not found");
                    });
        }


        // ----------------------------------------------
        // Check Mobile Number
        // ----------------------------------------------

        if (!supplier.getMobileNumber()
                .equals(dto.getMobileNumber())
                && supplierRepo.existsByMobileNumber(
                dto.getMobileNumber()
        )) {

            log.info(
                    "SERVICE - supplier mobile number already exists..."
            );

            throw new DuplicateResourceException(
                    "Supplier with this mobile number already exists"
            );
        }


        // ----------------------------------------------
        // Check Email
        // ----------------------------------------------

        if (dto.getEmail() != null
                && !dto.getEmail().isBlank()
                && !dto.getEmail().equals(
                supplier.getEmail()
        )
                && supplierRepo.existsByEmail(
                dto.getEmail()
        )) {

            log.info(
                    "SERVICE - supplier email already exists..."
            );

            throw new DuplicateResourceException(
                    "Supplier with this email already exists"
            );
        }


        // ----------------------------------------------
        // Check GST Number
        // ----------------------------------------------

        if (dto.getGstNumber() != null
                && !dto.getGstNumber().isBlank()) {
            String trimmedGst = dto.getGstNumber().trim().toUpperCase();
            if (!trimmedGst.equalsIgnoreCase(supplier.getGstNumber())
                    && supplierRepo.existsByUserAndGstNumberAndPublicIdNot(
                    currentUser,
                    trimmedGst,
                    supplier.getPublicId()
            )) {

                log.info(
                        "SERVICE - supplier GST number already exists..."
                );

                throw new DuplicateResourceException(
                        "Supplier with this GST number already exists"
                );
            }
        }


        // ----------------------------------------------
        // Update Supplier Fields
        // ----------------------------------------------

        supplier.setSupplierName(
                dto.getSupplierName()
        );

        supplier.setMobileNumber(
                dto.getMobileNumber()
        );

        supplier.setContactPerson(
                dto.getContactPerson() != null && !dto.getContactPerson().isBlank() ? dto.getContactPerson().trim() : null
        );

        supplier.setAlternateMobileNumber(
                dto.getAlternateMobileNumber() != null && !dto.getAlternateMobileNumber().isBlank() ? dto.getAlternateMobileNumber().trim() : null
        );

        supplier.setEmail(
                dto.getEmail() != null && !dto.getEmail().isBlank() ? dto.getEmail().trim() : null
        );

        supplier.setGstNumber(
                dto.getGstNumber() != null && !dto.getGstNumber().isBlank() ? dto.getGstNumber().trim().toUpperCase() : null
        );

        supplier.setOpeningBalance(
                dto.getOpeningBalance()
        );

        supplier.setPaymentTerms(
                dto.getPaymentTerms()
        );


        // ----------------------------------------------
        // Explicitly Handle Address
        // ----------------------------------------------

        if (dto.getAddress() != null
                && dto.getAddress().getAddressLine1() != null
                && !dto.getAddress().getAddressLine1().isBlank()) {

            log.info(
                    "SERVICE - supplier address found..."
            );

            SupplierAddress address =
                    supplier.getAddress();

            if (address == null) {

                address =
                        modelMapper.map(
                                dto.getAddress(),
                                SupplierAddress.class
                        );

                if (address.getCity() == null || address.getCity().isBlank()) {
                    address.setCity("Unknown");
                }
                if (address.getState() == null || address.getState().isBlank()) {
                    address.setState("Unknown");
                }
                if (address.getCountry() == null || address.getCountry().isBlank()) {
                    address.setCountry("India");
                }
                if (address.getPincode() == null || address.getPincode().isBlank()) {
                    address.setPincode("000000");
                }

                address.setSupplier(supplier);

                supplier.setAddress(address);

            } else {

                modelMapper.map(
                        dto.getAddress(),
                        address
                );

                address.setSupplier(supplier);
            }
        }


        // ----------------------------------------------
        // Save
        // ----------------------------------------------

        supplier =
                supplierRepo.save(supplier);


        log.info(
                "SERVICE - supplier updated successfully..."
        );


        return mapToResponse(supplier);
    }


    // ==================================================
    // DEACTIVATE SUPPLIER
    // ==================================================

    @Override
    @Transactional
    public void deactivateSupplier(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in deactivateSupplier..."
        );


        User currentUser = currentUserService.getCurrentUser();
        Supplier supplier;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            supplier = supplierRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        } else {
            supplier = supplierRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        }

        if (!supplier.getIsActive()) {

            log.info(
                    "SERVICE - supplier is already inactive..."
            );

            return;
        }


        supplier.setIsActive(false);

        supplierRepo.save(supplier);


        log.info(
                "SERVICE - supplier deactivated successfully..."
        );
    }


    // ==================================================
    // REACTIVATE SUPPLIER
    // ==================================================

    @Override
    @Transactional
    public void reactivateSupplier(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in reactivateSupplier..."
        );


        User currentUser = currentUserService.getCurrentUser();
        Supplier supplier;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            supplier = supplierRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        } else {
            supplier = supplierRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        }

        if (supplier.getIsActive()) {

            log.info(
                    "SERVICE - supplier is already active..."
            );

            return;
        }


        supplier.setIsActive(true);

        supplierRepo.save(supplier);


        log.info(
                "SERVICE - supplier activated successfully..."
        );
    }


    // ==================================================
    // ENTITY → RESPONSE DTO
    // ==================================================

    private ResponseSupplierDTO mapToResponse(
            Supplier supplier
    ) {

        log.info(
                "SERVICE - mapping supplier to response DTO..."
        );


        ResponseSupplierDTO response =
                modelMapper.map(
                        supplier,
                        ResponseSupplierDTO.class
                );


        // ----------------------------------------------
        // Explicitly Handle Address
        // ----------------------------------------------

        if (supplier.getAddress() != null) {

            response.setAddress(
                    modelMapper.map(
                            supplier.getAddress(),
                            FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseSupplierAddressDTO.class
                    )
            );
        }


        return response;
    }


    // ==================================================
    // GET SUPPLIER STATEMENT (LEDGER)
    // ==================================================

    @Override
    @Transactional(readOnly = true)
    public ResponsePartyStatementDTO getSupplierStatement(
            UUID publicId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        log.info("SERVICE - generating statement for supplier publicId={}, fromDate={}, toDate={}", publicId, fromDate, toDate);

        User currentUser = currentUserService.getCurrentUser();
        Supplier supplier;
        if (currentUser.getRole() == UserRole.ADMIN) {
            supplier = supplierRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        } else {
            supplier = supplierRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        }

        // Fetch all purchases for this supplier
        List<Purchase> allPurchases = purchaseRepo.findBySupplier(supplier);

        // Fetch all payments for this supplier
        List<PurchasePayment> allPayments;
        if (currentUser.getRole() == UserRole.ADMIN) {
            allPayments = purchasePaymentRepo.findByPurchase_Supplier_PublicIdOrderByPaymentDateDesc(supplier.getPublicId());
        } else {
            allPayments = purchasePaymentRepo.findByUserAndPurchase_Supplier_PublicIdOrderByPaymentDateDesc(currentUser, supplier.getPublicId());
        }

        // Initial base opening balance
        BigDecimal initialOpening = supplier.getOpeningBalance() != null ? supplier.getOpeningBalance() : BigDecimal.ZERO;

        // Calculate opening balance at fromDate (transactions prior to fromDate)
        // For AP: purchases increase debt (credit), payments decrease debt (debit)
        BigDecimal periodOpeningBalance = initialOpening;
        if (fromDate != null) {
            for (Purchase p : allPurchases) {
                if (p.getPurchaseDate().isBefore(fromDate)) {
                    periodOpeningBalance = periodOpeningBalance.add(p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO);
                }
            }
            for (PurchasePayment p : allPayments) {
                if (p.getPaymentDate().isBefore(fromDate)) {
                    periodOpeningBalance = periodOpeningBalance.subtract(p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO);
                }
            }
        }

        // Filter transactions within period [fromDate, toDate]
        List<LedgerEntryDTO> periodEntries = new ArrayList<>();
        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (Purchase p : allPurchases) {
            if ((fromDate == null || !p.getPurchaseDate().isBefore(fromDate)) &&
                (toDate == null || !p.getPurchaseDate().isAfter(toDate))) {
                BigDecimal credit = p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO;
                totalBilled = totalBilled.add(credit);
                String desc = p.getRawMaterial() + " (" + p.getWeight() + " " + p.getUnit() + " @ ₹" + p.getRatePerUnit() + ")";
                periodEntries.add(LedgerEntryDTO.builder()
                        .date(p.getPurchaseDate())
                        .entryType("BILL")
                        .documentNumber(p.getPurchaseNumber())
                        .referenceNumber(p.getSupplierInvoiceNumber())
                        .description(desc)
                        .debit(BigDecimal.ZERO)
                        .credit(credit)
                        .paymentMode(null)
                        .remarks("Purchase Bill")
                        .build());
            }
        }

        for (PurchasePayment p : allPayments) {
            if ((fromDate == null || !p.getPaymentDate().isBefore(fromDate)) &&
                (toDate == null || !p.getPaymentDate().isAfter(toDate))) {
                BigDecimal debit = p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO;
                totalPaid = totalPaid.add(debit);
                String desc = "Payment Made" + (p.getPurchase() != null && p.getPurchase().getPurchaseNumber() != null ? " (" + p.getPurchase().getPurchaseNumber() + ")" : "");
                periodEntries.add(LedgerEntryDTO.builder()
                        .date(p.getPaymentDate())
                        .entryType("PAYMENT")
                        .documentNumber(p.getPaymentNumber())
                        .referenceNumber(p.getReferenceNumber())
                        .description(desc)
                        .debit(debit)
                        .credit(BigDecimal.ZERO)
                        .paymentMode(p.getPaymentMode() != null ? p.getPaymentMode().name() : null)
                        .remarks(p.getRemarks())
                        .build());
            }
        }

        // Sort chronologically ascending: date asc, BILL before PAYMENT
        periodEntries.sort((a, b) -> {
            int dateComp = a.getDate().compareTo(b.getDate());
            if (dateComp != 0) return dateComp;
            if ("BILL".equals(a.getEntryType()) && !"BILL".equals(b.getEntryType())) return -1;
            if (!"BILL".equals(a.getEntryType()) && "BILL".equals(b.getEntryType())) return 1;
            return 0;
        });

        // Compute running balance (for AP: runningBalance = runningBalance + credit (bills) - debit (payments))
        BigDecimal runningBalance = periodOpeningBalance;
        for (LedgerEntryDTO entry : periodEntries) {
            runningBalance = runningBalance.add(entry.getCredit()).subtract(entry.getDebit());
            entry.setRunningBalance(runningBalance);
        }

        // Format address string
        String addressStr = null;
        String cityStr = null;
        if (supplier.getAddress() != null) {
            SupplierAddress addr = supplier.getAddress();
            cityStr = addr.getCity();
            StringBuilder sb = new StringBuilder();
            if (addr.getAddressLine1() != null) sb.append(addr.getAddressLine1());
            if (addr.getAddressLine2() != null && !addr.getAddressLine2().isBlank()) sb.append(", ").append(addr.getAddressLine2());
            if (addr.getCity() != null) sb.append(", ").append(addr.getCity());
            if (addr.getState() != null) sb.append(", ").append(addr.getState());
            if (addr.getPincode() != null) sb.append(" - ").append(addr.getPincode());
            addressStr = sb.toString();
        }

        return ResponsePartyStatementDTO.builder()
                .partyPublicId(supplier.getPublicId())
                .partyName(supplier.getSupplierName())
                .mobileNumber(supplier.getMobileNumber())
                .email(supplier.getEmail())
                .gstNumber(supplier.getGstNumber())
                .city(cityStr)
                .address(addressStr)
                .openingBalance(periodOpeningBalance)
                .totalBilled(totalBilled)
                .totalPaid(totalPaid)
                .outstandingBalance(runningBalance)
                .entries(periodEntries)
                .build();
    }
}