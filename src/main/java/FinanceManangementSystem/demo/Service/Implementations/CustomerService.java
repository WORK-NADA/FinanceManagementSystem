package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;


import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Exceptions.DuplicateResourceException;
import FinanceManangementSystem.demo.Exceptions.InvalidStateException;
import FinanceManangementSystem.demo.Model.Customer;
import FinanceManangementSystem.demo.Model.CustomerAddress;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestCustomerDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseCustomerDTO;
import FinanceManangementSystem.demo.Repository.CustomerRepository;
import FinanceManangementSystem.demo.Service.CustomerServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import FinanceManangementSystem.demo.Model.Sale;
import FinanceManangementSystem.demo.Model.SalePayment;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePartyStatementDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePartyStatementDTO.LedgerEntryDTO;
import FinanceManangementSystem.demo.Repository.SaleRepository;
import FinanceManangementSystem.demo.Repository.SalePaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService
        implements CustomerServiceInterface {

    private final CustomerRepository customerRepo;

    private final CurrentUserService currentUserService;

    private final ModelMapper modelMapper;

    private final SaleRepository saleRepo;

    private final SalePaymentRepository salePaymentRepo;


    // =========================================================
    // ADD CUSTOMER
    // =========================================================

    @Override
    @Transactional
    public ResponseCustomerDTO addCustomer(
            RequestCustomerDTO dto
    ) {

        log.info(
                "SERVICE - request came in addCustomer..."
        );


        // -----------------------------------------------------
        // NORMALIZE VALUES
        // -----------------------------------------------------

        String mobileNumber =
                dto.getMobileNumber().trim();

        String email =
                normalizeEmail(
                        dto.getEmail()
                );

        String gstNumber =
                normalizeGst(
                        dto.getGstNumber()
                );


        // -----------------------------------------------------
        // RESOLVE CURRENT USER (needed for per-user checks)
        // -----------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();


        // -----------------------------------------------------
        // CHECK DUPLICATE MOBILE
        // -----------------------------------------------------

        if (customerRepo.existsByUserAndMobileNumber(
                currentUser,
                mobileNumber
        )) {

            log.warn(
                    "SERVICE - mobile number already exists..."
            );

            throw new DuplicateResourceException(
                    "Customer with this mobile number already exists"
            );
        }


        // -----------------------------------------------------
        // CHECK DUPLICATE EMAIL
        // -----------------------------------------------------

        if (email != null
                && customerRepo.existsByUserAndEmail(currentUser, email)) {

            log.warn(
                    "SERVICE - email already exists..."
            );

            throw new DuplicateResourceException(
                    "Customer with this email already exists"
            );
        }


        // -----------------------------------------------------
        // CHECK DUPLICATE GST
        // -----------------------------------------------------

        if (gstNumber != null
                && customerRepo.existsByUserAndGstNumber(currentUser, gstNumber)) {

            log.warn(
                    "SERVICE - GST number already exists..."
            );

            throw new DuplicateResourceException(
                    "Customer with this GST number already exists"
            );
        }


        // -----------------------------------------------------
        // CREATE CUSTOMER
        // -----------------------------------------------------

        Customer customer =
                new Customer();

        customer.setUser(currentUser);

        customer.setCustomerName(
                dto.getCustomerName().trim()
        );


        customer.setMobileNumber(
                mobileNumber
        );


        customer.setContactPerson(
                normalizeString(
                        dto.getContactPerson()
                )
        );


        customer.setAlternateMobileNumber(
                normalizeString(
                        dto.getAlternateMobileNumber()
                )
        );


        customer.setEmail(
                email
        );


        customer.setGstNumber(
                gstNumber
        );


        // -----------------------------------------------------
        // OPENING BALANCE
        // -----------------------------------------------------

        customer.setOpeningBalance(
                dto.getOpeningBalance()
        );


        // -----------------------------------------------------
        // PAYMENT TERMS
        // -----------------------------------------------------

        customer.setPaymentTerms(
                dto.getPaymentTerms()
        );


        // -----------------------------------------------------
        // SERVICE CONTROLLED STATUS
        // -----------------------------------------------------

        customer.setIsActive(
                true
        );


        // -----------------------------------------------------
        // CUSTOMER ADDRESS
        // -----------------------------------------------------

        setCustomerAddress(
                customer,
                dto.getAddress()
        );


        // -----------------------------------------------------
        // SAVE CUSTOMER
        // -----------------------------------------------------

        log.info(
                "SERVICE - saving customer..."
        );


        customer =
                customerRepo.save(
                        customer
                );


        log.info(
                "SERVICE - customer added successfully..."
        );


        return mapToResponse(
                customer
        );
    }


    // =========================================================
    // GET CUSTOMER BY PUBLIC ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseCustomerDTO getCustomerByPublicId(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in getCustomerByPublicId..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Customer customer;

        if (currentUser.getRole() == UserRole.ADMIN) {
            customer = customerRepo.findByPublicId(publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - customer not found...");
                        return new ResourceNotFoundException("Customer not found");
                    });
        } else {
            customer = customerRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - customer not found for current user...");
                        return new ResourceNotFoundException("Customer not found");
                    });
        }

        return mapToResponse(customer);
    }


    // =========================================================
    // GET ALL CUSTOMERS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseCustomerDTO> getAllCustomers() {

        log.info(
                "SERVICE - request came in getAllCustomers..."
        );


        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return customerRepo.findAll().stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        return customerRepo
                .findByUser(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET ALL ACTIVE CUSTOMERS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseCustomerDTO> getAllActiveCustomers() {

        log.info(
                "SERVICE - request came in getAllActiveCustomers..."
        );


        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return customerRepo.findByIsActiveTrue().stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        return customerRepo
                .findByUserAndIsActiveTrue(currentUser)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // UPDATE CUSTOMER
    // =========================================================

    @Override
    @Transactional
    public ResponseCustomerDTO updateCustomer(
            UUID publicId,
            RequestCustomerDTO dto
    ) {

        log.info(
                "SERVICE - request came in updateCustomer..."
        );


        // -----------------------------------------------------
        // FIND CUSTOMER
        // -----------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        Customer customer;

        if (currentUser.getRole() == UserRole.ADMIN) {
            customer = customerRepo.findByPublicId(publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - customer not found...");
                        return new ResourceNotFoundException("Customer not found");
                    });
        } else {
            customer = customerRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - customer not found for current user...");
                        return new ResourceNotFoundException("Customer not found");
                    });
        }


        // -----------------------------------------------------
        // CHECK CUSTOMER STATUS
        // -----------------------------------------------------

        if (!Boolean.TRUE.equals(
                customer.getIsActive()
        )) {

            log.info(
                    "SERVICE - inactive customer cannot be updated..."
            );

            throw new InvalidStateException(
                    "Inactive customer cannot be updated"
            );
        }


        // -----------------------------------------------------
        // NORMALIZE VALUES
        // -----------------------------------------------------

        String mobileNumber =
                dto.getMobileNumber().trim();

        String email =
                normalizeEmail(
                        dto.getEmail()
                );

        String gstNumber =
                normalizeGst(
                        dto.getGstNumber()
                );


        // -----------------------------------------------------
        // CHECK MOBILE DUPLICATE
        // -----------------------------------------------------

        if (customerRepo
                .existsByUserAndMobileNumberAndPublicIdNot(
                        currentUser,
                        mobileNumber,
                        publicId
                )) {

            log.info(
                    "SERVICE - mobile number already exists for another customer..."
            );

            throw new DuplicateResourceException(
                    "Customer with this mobile number already exists"
            );
        }


        // -----------------------------------------------------
        // CHECK EMAIL DUPLICATE
        // -----------------------------------------------------

        if (email != null
                && customerRepo
                .existsByUserAndEmailAndPublicIdNot(
                        currentUser,
                        email,
                        publicId
                )) {

            log.info(
                    "SERVICE - email already exists for another customer..."
            );

            throw new DuplicateResourceException(
                    "Customer with this email already exists"
            );
        }


        // -----------------------------------------------------
        // CHECK GST DUPLICATE
        // -----------------------------------------------------

        if (gstNumber != null
                && customerRepo
                .existsByUserAndGstNumberAndPublicIdNot(
                        currentUser,
                        gstNumber,
                        publicId
                )) {

            log.info(
                    "SERVICE - GST number already exists for another customer..."
            );

            throw new DuplicateResourceException(
                    "Customer with this GST number already exists"
            );
        }


        // -----------------------------------------------------
        // UPDATE CUSTOMER DETAILS
        // -----------------------------------------------------

        customer.setCustomerName(
                dto.getCustomerName().trim()
        );


        customer.setMobileNumber(
                mobileNumber
        );


        customer.setContactPerson(
                normalizeString(
                        dto.getContactPerson()
                )
        );


        customer.setAlternateMobileNumber(
                normalizeString(
                        dto.getAlternateMobileNumber()
                )
        );


        customer.setEmail(
                email
        );


        customer.setGstNumber(
                gstNumber
        );


        // -----------------------------------------------------
        // UPDATE OPENING BALANCE
        // -----------------------------------------------------

        customer.setOpeningBalance(
                dto.getOpeningBalance()
        );


        // -----------------------------------------------------
        // UPDATE PAYMENT TERMS
        // -----------------------------------------------------

        customer.setPaymentTerms(
                dto.getPaymentTerms()
        );


        // -----------------------------------------------------
        // UPDATE ADDRESS
        // -----------------------------------------------------

        updateCustomerAddress(
                customer,
                dto.getAddress()
        );


        // -----------------------------------------------------
        // SAVE CUSTOMER
        // -----------------------------------------------------

        customer =
                customerRepo.save(
                        customer
                );


        log.info(
                "SERVICE - customer updated successfully..."
        );


        return mapToResponse(
                customer
        );
    }


    // =========================================================
    // DEACTIVATE CUSTOMER
    // =========================================================

    @Override
    @Transactional
    public void deactivateCustomer(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in deactivateCustomer..."
        );


        // -----------------------------------------------------
        // FIND CUSTOMER
        // -----------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        Customer customer;

        if (currentUser.getRole() == UserRole.ADMIN) {
            customer = customerRepo.findByPublicId(publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - customer not found...");
                        return new ResourceNotFoundException("Customer not found");
                    });
        } else {
            customer = customerRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - customer not found for current user...");
                        return new ResourceNotFoundException("Customer not found");
                    });
        }


        // -----------------------------------------------------
        // CHECK STATUS
        // -----------------------------------------------------

        if (!Boolean.TRUE.equals(
                customer.getIsActive()
        )) {

            log.info(
                    "SERVICE - customer is already inactive..."
            );

            return;
        }


        // -----------------------------------------------------
        // DEACTIVATE
        // -----------------------------------------------------

        customer.setIsActive(
                false
        );


        customerRepo.save(
                customer
        );


        log.info(
                "SERVICE - customer deactivated successfully..."
        );
    }


    // =========================================================
    // REACTIVATE CUSTOMER
    // =========================================================

    @Override
    @Transactional
    public void reactivateCustomer(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in reactivateCustomer..."
        );


        // -----------------------------------------------------
        // FIND CUSTOMER
        // -----------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        Customer customer;

        if (currentUser.getRole() == UserRole.ADMIN) {
            customer = customerRepo.findByPublicId(publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - customer not found...");
                        return new ResourceNotFoundException("Customer not found");
                    });
        } else {
            customer = customerRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - customer not found for current user...");
                        return new ResourceNotFoundException("Customer not found");
                    });
        }


        // -----------------------------------------------------
        // CHECK STATUS
        // -----------------------------------------------------

        if (Boolean.TRUE.equals(
                customer.getIsActive()
        )) {

            log.info(
                    "SERVICE - customer is already active..."
            );

            return;
        }


        // -----------------------------------------------------
        // REACTIVATE
        // -----------------------------------------------------

        customer.setIsActive(
                true
        );


        customerRepo.save(
                customer
        );


        log.info(
                "SERVICE - customer reactivated successfully..."
        );
    }


    // =========================================================
    // SET CUSTOMER ADDRESS
    // =========================================================

    private void setCustomerAddress(
            Customer customer,
            RequestCustomerDTO.CustomerAddressDTO dto
    ) {

        if (dto == null || dto.getAddressLine1() == null || dto.getAddressLine1().isBlank()) {
            return;
        }


        CustomerAddress address =
                new CustomerAddress();


        address.setCustomer(
                customer
        );


        address.setAddressLine1(
                dto.getAddressLine1().trim()
        );


        address.setAddressLine2(
                normalizeString(
                        dto.getAddressLine2()
                )
        );


        address.setCity(
                dto.getCity() != null && !dto.getCity().isBlank()
                        ? dto.getCity().trim()
                        : "Unknown"
        );


        address.setState(
                dto.getState() != null && !dto.getState().isBlank()
                        ? dto.getState().trim()
                        : "Unknown"
        );


        address.setCountry(
                dto.getCountry() != null && !dto.getCountry().isBlank()
                        ? dto.getCountry().trim()
                        : "India"
        );


        address.setPincode(
                dto.getPincode() != null && !dto.getPincode().isBlank()
                        ? dto.getPincode().trim()
                        : "000000"
        );


        customer.setAddress(
                address
        );
    }


    // =========================================================
    // UPDATE CUSTOMER ADDRESS
    // =========================================================

    private void updateCustomerAddress(
            Customer customer,
            RequestCustomerDTO.CustomerAddressDTO dto
    ) {

        if (dto == null || dto.getAddressLine1() == null || dto.getAddressLine1().isBlank()) {
            return;
        }


        CustomerAddress address =
                customer.getAddress();


        // -----------------------------------------------------
        // CREATE ADDRESS IF IT DOES NOT EXIST
        // -----------------------------------------------------

        if (address == null) {

            address =
                    new CustomerAddress();


            address.setCustomer(
                    customer
            );


            customer.setAddress(
                    address
            );
        }


        // -----------------------------------------------------
        // UPDATE ADDRESS
        // -----------------------------------------------------

        address.setAddressLine1(
                dto.getAddressLine1().trim()
        );


        address.setAddressLine2(
                normalizeString(
                        dto.getAddressLine2()
                )
        );


        address.setCity(
                dto.getCity() != null && !dto.getCity().isBlank()
                        ? dto.getCity().trim()
                        : "Unknown"
        );


        address.setState(
                dto.getState() != null && !dto.getState().isBlank()
                        ? dto.getState().trim()
                        : "Unknown"
        );


        address.setCountry(
                dto.getCountry() != null && !dto.getCountry().isBlank()
                        ? dto.getCountry().trim()
                        : "India"
        );


        address.setPincode(
                dto.getPincode() != null && !dto.getPincode().isBlank()
                        ? dto.getPincode().trim()
                        : "000000"
        );
    }


    // =========================================================
    // ENTITY → RESPONSE DTO
    // =========================================================

    private ResponseCustomerDTO mapToResponse(
            Customer customer
    ) {

        log.info(
                "SERVICE - mapping customer to response DTO..."
        );


        // -----------------------------------------------------
        // MAP CUSTOMER
        // -----------------------------------------------------

        ResponseCustomerDTO response =
                modelMapper.map(
                        customer,
                        ResponseCustomerDTO.class
                );


        // -----------------------------------------------------
        // MAP ADDRESS
        // -----------------------------------------------------

        CustomerAddress address =
                customer.getAddress();


        if (address != null) {

            ResponseCustomerDTO.CustomerAddressDetails
                    addressDetails =
                    new ResponseCustomerDTO.CustomerAddressDetails();


            addressDetails.setAddressLine1(
                    address.getAddressLine1()
            );


            addressDetails.setAddressLine2(
                    address.getAddressLine2()
            );


            addressDetails.setCity(
                    address.getCity()
            );


            addressDetails.setState(
                    address.getState()
            );


            addressDetails.setCountry(
                    address.getCountry()
            );


            addressDetails.setPincode(
                    address.getPincode()
            );


            response.setAddress(
                    addressDetails
            );
        }


        return response;
    }


    // =========================================================
    // NORMALIZE STRING
    // =========================================================

    private String normalizeString(
            String value
    ) {

        if (value == null) {
            return null;
        }


        String trimmed =
                value.trim();


        return trimmed.isBlank()
                ? null
                : trimmed;
    }


    // =========================================================
    // NORMALIZE EMAIL
    // =========================================================

    private String normalizeEmail(
            String email
    ) {

        String normalized =
                normalizeString(
                        email
                );


        if (normalized == null) {
            return null;
        }


        return normalized.toLowerCase();
    }


    // =========================================================
    // NORMALIZE GST
    // =========================================================

    private String normalizeGst(
            String gstNumber
    ) {

        String normalized =
                normalizeString(
                        gstNumber
                );


        if (normalized == null) {
            return null;
        }


        return normalized.toUpperCase();
    }


    // =========================================================
    // GET CUSTOMER STATEMENT (LEDGER)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponsePartyStatementDTO getCustomerStatement(
            UUID publicId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        log.info("SERVICE - generating statement for customer publicId={}, fromDate={}, toDate={}", publicId, fromDate, toDate);

        User currentUser = currentUserService.getCurrentUser();
        Customer customer;
        if (currentUser.getRole() == UserRole.ADMIN) {
            customer = customerRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        } else {
            customer = customerRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }

        // Fetch all sales for this customer
        List<Sale> allSales = saleRepo.findByCustomer(customer);

        // Fetch all payments for this customer
        List<SalePayment> allPayments;
        if (currentUser.getRole() == UserRole.ADMIN) {
            allPayments = salePaymentRepo.findBySale_Customer_PublicIdOrderByPaymentDateDesc(customer.getPublicId());
        } else {
            allPayments = salePaymentRepo.findByUserAndSale_Customer_PublicIdOrderByPaymentDateDesc(currentUser, customer.getPublicId());
        }

        // Initial base opening balance
        BigDecimal initialOpening = customer.getOpeningBalance() != null ? customer.getOpeningBalance() : BigDecimal.ZERO;

        // Calculate opening balance at fromDate (transactions prior to fromDate)
        BigDecimal periodOpeningBalance = initialOpening;
        if (fromDate != null) {
            for (Sale s : allSales) {
                if (s.getSaleDate().isBefore(fromDate)) {
                    periodOpeningBalance = periodOpeningBalance.add(s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO);
                }
            }
            for (SalePayment p : allPayments) {
                if (p.getPaymentDate().isBefore(fromDate)) {
                    periodOpeningBalance = periodOpeningBalance.subtract(p.getAmountReceived() != null ? p.getAmountReceived() : BigDecimal.ZERO);
                }
            }
        }

        // Filter transactions within period [fromDate, toDate]
        List<LedgerEntryDTO> periodEntries = new ArrayList<>();
        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (Sale s : allSales) {
            if ((fromDate == null || !s.getSaleDate().isBefore(fromDate)) &&
                (toDate == null || !s.getSaleDate().isAfter(toDate))) {
                BigDecimal debit = s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO;
                totalBilled = totalBilled.add(debit);
                String desc = s.getRawMaterial() + " (" + s.getWeight() + " " + s.getUnit() + " @ ₹" + s.getRatePerUnit() + ")";
                periodEntries.add(LedgerEntryDTO.builder()
                        .date(s.getSaleDate())
                        .entryType("INVOICE")
                        .documentNumber(s.getSaleNumber())
                        .referenceNumber(s.getCustomerInvoiceNumber())
                        .description(desc)
                        .debit(debit)
                        .credit(BigDecimal.ZERO)
                        .paymentMode(null)
                        .remarks("Sale Invoice")
                        .build());
            }
        }

        for (SalePayment p : allPayments) {
            if ((fromDate == null || !p.getPaymentDate().isBefore(fromDate)) &&
                (toDate == null || !p.getPaymentDate().isAfter(toDate))) {
                BigDecimal credit = p.getAmountReceived() != null ? p.getAmountReceived() : BigDecimal.ZERO;
                totalPaid = totalPaid.add(credit);
                String desc = "Payment Received" + (p.getSale() != null && p.getSale().getSaleNumber() != null ? " (" + p.getSale().getSaleNumber() + ")" : "");
                periodEntries.add(LedgerEntryDTO.builder()
                        .date(p.getPaymentDate())
                        .entryType("PAYMENT")
                        .documentNumber(p.getPaymentNumber())
                        .referenceNumber(p.getReferenceNumber())
                        .description(desc)
                        .debit(BigDecimal.ZERO)
                        .credit(credit)
                        .paymentMode(p.getPaymentMode() != null ? p.getPaymentMode().name() : null)
                        .remarks(p.getRemarks())
                        .build());
            }
        }

        // Sort chronologically ascending: date asc, INVOICE before PAYMENT
        periodEntries.sort((a, b) -> {
            int dateComp = a.getDate().compareTo(b.getDate());
            if (dateComp != 0) return dateComp;
            if ("INVOICE".equals(a.getEntryType()) && !"INVOICE".equals(b.getEntryType())) return -1;
            if (!"INVOICE".equals(a.getEntryType()) && "INVOICE".equals(b.getEntryType())) return 1;
            return 0;
        });

        // Compute running balance
        BigDecimal runningBalance = periodOpeningBalance;
        for (LedgerEntryDTO entry : periodEntries) {
            runningBalance = runningBalance.add(entry.getDebit()).subtract(entry.getCredit());
            entry.setRunningBalance(runningBalance);
        }

        // Format address string
        String addressStr = null;
        String cityStr = null;
        if (customer.getAddress() != null) {
            CustomerAddress addr = customer.getAddress();
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
                .partyPublicId(customer.getPublicId())
                .partyName(customer.getCustomerName())
                .mobileNumber(customer.getMobileNumber())
                .email(customer.getEmail())
                .gstNumber(customer.getGstNumber())
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