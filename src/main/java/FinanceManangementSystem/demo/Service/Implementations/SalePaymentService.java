package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;

import FinanceManangementSystem.demo.Enums.DocumentType;
import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Model.Customer;
import FinanceManangementSystem.demo.Model.Sale;
import FinanceManangementSystem.demo.Model.SalePayment;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestSalePaymentDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseSalePaymentDTO;
import FinanceManangementSystem.demo.Repository.CustomerRepository;
import FinanceManangementSystem.demo.Repository.SalePaymentRepository;
import FinanceManangementSystem.demo.Repository.SaleRepository;
import FinanceManangementSystem.demo.Service.SalePaymentServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalePaymentService
        implements SalePaymentServiceInterface {

    private final SalePaymentRepository salePaymentRepo;

    private final SaleRepository saleRepo;

    private final CurrentUserService currentUserService;

    private final CustomerRepository customerRepo;

    private final DocumentSequenceService documentSequenceService;

    private final ModelMapper modelMapper;


    // =========================================================
    // ADD PAYMENT
    // =========================================================

    @Override
    @Transactional
    public ResponseSalePaymentDTO addPayment(
            RequestSalePaymentDTO dto
    ) {

        log.info(
                "SERVICE - request came in addPayment for sale..."
        );


        // -----------------------------------------------------
        // FIND SALE
        // -----------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        Sale sale =
                saleRepo
                        .findByUserAndPublicId(
                                currentUser,
                                dto.getSalePublicId()
                        )
                        .orElseThrow(() -> {
                            log.info("SERVICE - sale not found...");
                            return new ResourceNotFoundException("Sale not found");
                        });


        // -----------------------------------------------------
        // VALIDATE AMOUNT
        // -----------------------------------------------------

        BigDecimal currentReceivedAmount =
                salePaymentRepo
                        .sumReceivedAmountBySale(
                                sale
                        );

        BigDecimal newTotalReceived =
                currentReceivedAmount
                        .add(
                                dto.getAmountReceived()
                        );

        if (newTotalReceived.compareTo(sale.getTotalAmount()) > 0) {

            log.info(
                    "SERVICE - payment amount exceeds pending balance..."
            );

            BigDecimal currentPending =
                    sale
                            .getTotalAmount()
                            .subtract(
                                    currentReceivedAmount
                            )
                            .max(
                                    BigDecimal.ZERO
                            );

            throw new InvalidRequestException(
                    "Payment amount exceeds remaining pending amount of " + currentPending
            );
        }


        // -----------------------------------------------------
        // GENERATE PAYMENT NUMBER (system-assigned, sequential)
        // -----------------------------------------------------

        int year =
                dto.getPaymentDate()
                        .getYear();

        String paymentNumber =
                documentSequenceService
                        .generateDocumentNumber(
                                DocumentType.CUSTOMER_RECEIPT,
                                year
                        );


        // -----------------------------------------------------
        // CREATE PAYMENT ENTITY
        // -----------------------------------------------------

        SalePayment payment =
                new SalePayment();

        payment.setUser(currentUser);

        payment.setSale(
                sale
        );

        payment.setAmountReceived(
                dto.getAmountReceived()
        );

        payment.setPaymentDate(
                dto.getPaymentDate()
        );

        payment.setPaymentMode(
                dto.getPaymentMode()
        );

        payment.setPaymentNumber(
                paymentNumber
        );

        payment.setReferenceNumber(
                dto.getReferenceNumber() != null ? dto.getReferenceNumber().trim() : null
        );

        payment.setRemarks(
                dto.getRemarks() != null ? dto.getRemarks().trim() : null
        );

        payment =
                salePaymentRepo.save(
                        payment
                );


        // -----------------------------------------------------
        // UPDATE SALE PAYMENT STATUS
        // -----------------------------------------------------

        if (newTotalReceived.compareTo(sale.getTotalAmount()) >= 0) {

            sale.setPaymentStatus(
                    PaymentStatus.COMPLETED
            );

        } else if (newTotalReceived.compareTo(BigDecimal.ZERO) > 0) {

            sale.setPaymentStatus(
                    PaymentStatus.PARTIALLY_PAID
            );

        } else {

            sale.setPaymentStatus(
                    PaymentStatus.PENDING
            );
        }

        saleRepo.save(
                sale
        );


        log.info(
                "SERVICE - sale payment added successfully..."
        );


        return mapToResponse(
                payment,
                sale,
                newTotalReceived
        );
    }


    // =========================================================
    // GET PAYMENT BY PUBLIC ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseSalePaymentDTO getPaymentByPublicId(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in getPaymentByPublicId for sale..."
        );

        User currentUser = currentUserService.getCurrentUser();

        SalePayment payment;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            payment = salePaymentRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sale payment not found"));
        } else {
            payment = salePaymentRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sale payment not found"));
        }

        Sale sale =
                payment.getSale();

        BigDecimal receivedAmount =
                salePaymentRepo
                        .sumReceivedAmountBySale(
                                sale
                        );

        return mapToResponse(
                payment,
                sale,
                receivedAmount
        );
    }


    // =========================================================
    // GET PAYMENTS BY SALE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseSalePaymentDTO> getPaymentsBySale(
            UUID salePublicId
    ) {

        log.info(
                "SERVICE - request came in getPaymentsBySale..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Sale sale;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            sale = saleRepo.findByPublicId(salePublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
        } else {
            sale = saleRepo.findByUserAndPublicId(currentUser, salePublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
        }

        BigDecimal receivedAmount =
                salePaymentRepo
                        .sumReceivedAmountBySale(
                                sale
                        );

        return salePaymentRepo
                .findBySaleOrderByPaymentDateDesc(
                        sale
                )
                .stream()
                .map(payment -> mapToResponse(payment, sale, receivedAmount))
                .toList();
    }


    // =========================================================
    // GET PAYMENTS BY CUSTOMER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseSalePaymentDTO> getPaymentsByCustomer(
            UUID customerPublicId
    ) {

        log.info(
                "SERVICE - request came in getPaymentsByCustomer..."
        );

        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            customerRepo.findByPublicIdAndIsActiveTrue(customerPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Active customer not found"));
            return salePaymentRepo
                    .findBySale_Customer_PublicIdOrderByPaymentDateDesc(customerPublicId)
                    .stream()
                    .map(payment -> {
                        Sale sale = payment.getSale();
                        BigDecimal receivedAmount = salePaymentRepo.sumReceivedAmountBySale(sale);
                        return mapToResponse(payment, sale, receivedAmount);
                    })
                    .toList();
        }

        customerRepo.findByUserAndPublicIdAndIsActiveTrue(currentUser, customerPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Active customer not found"));

        return salePaymentRepo
                .findByUserAndSale_Customer_PublicIdOrderByPaymentDateDesc(
                        currentUser,
                        customerPublicId
                )
                .stream()
                .map(payment -> {
                    Sale sale = payment.getSale();
                    BigDecimal receivedAmount = salePaymentRepo.sumReceivedAmountBySale(sale);
                    return mapToResponse(payment, sale, receivedAmount);
                })
                .toList();
    }


    // =========================================================
    // GET SALE PAYMENT SUMMARY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseSalePaymentDTO.SaleDetails getSalePaymentSummary(
            UUID salePublicId
    ) {

        log.info(
                "SERVICE - request came in getSalePaymentSummary..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Sale sale;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            sale = saleRepo.findByPublicId(salePublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
        } else {
            sale = saleRepo.findByUserAndPublicId(currentUser, salePublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
        }

        return buildSaleDetails(
                sale
        );
    }


    // =========================================================
    // GET ALL PENDING PAYMENTS (DASHBOARD)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseSalePaymentDTO.SaleDetails> getAllPendingPayments() {

        log.info(
                "SERVICE - request came in getAllPendingPayments for sales..."
        );

        User currentUser = currentUserService.getCurrentUser();

        List<Sale> pendingSales;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            pendingSales = saleRepo.findByPaymentStatusIn(List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
        } else {
            pendingSales = saleRepo.findByUserAndPaymentStatusIn(currentUser, List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
        }

        return pendingSales
                .stream()
                .map(this::buildSaleDetails)
                .toList();
    }


    // =========================================================
    // GET PENDING PAYMENTS BY CUSTOMER (DASHBOARD)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResponseSalePaymentDTO.SaleDetails> getPendingPaymentsByCustomer(
            UUID customerPublicId
    ) {

        log.info(
                "SERVICE - request came in getPendingPaymentsByCustomer..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Customer customer;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            customer = customerRepo.findByPublicIdAndIsActiveTrue(customerPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Active customer not found"));
            List<Sale> pendingSales = saleRepo.findByCustomerAndPaymentStatusIn(customer, List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
            return pendingSales.stream().map(this::buildSaleDetails).toList();
        }

        customer = customerRepo.findByUserAndPublicIdAndIsActiveTrue(currentUser, customerPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Active customer not found"));

        List<Sale> pendingSales = saleRepo.findByUser(currentUser).stream()
                .filter(s -> s.getCustomer() != null && s.getCustomer().getPublicId().equals(customer.getPublicId()))
                .filter(s -> s.getPaymentStatus() == PaymentStatus.PENDING || s.getPaymentStatus() == PaymentStatus.PARTIALLY_PAID)
                .toList();

        return pendingSales
                .stream()
                .map(this::buildSaleDetails)
                .toList();
    }


    // =========================================================
    // GET ALL PAYMENTS (PAGINATED)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseSalePaymentDTO> getAllPayments(@NonNull Pageable pageable) {

        log.info("SERVICE - request came in getAllPayments for sales...");

        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == UserRole.ADMIN) {
            return salePaymentRepo.findAll(pageable)
                    .map(payment -> {
                        Sale sale = payment.getSale();
                        BigDecimal receivedAmount = salePaymentRepo.sumReceivedAmountBySale(sale);
                        return mapToResponse(payment, sale, receivedAmount);
                    });
        }

        List<ResponseSalePaymentDTO> filtered = salePaymentRepo.findAll(pageable)
                .getContent()
                .stream()
                .filter(payment -> payment.getUser() != null && payment.getUser().equals(currentUser))
                .map(payment -> {
                    Sale sale = payment.getSale();
                    BigDecimal receivedAmount = salePaymentRepo.sumReceivedAmountBySale(sale);
                    return mapToResponse(payment, sale, receivedAmount);
                })
                .toList();

        return new PageImpl<>(filtered, pageable, filtered.size());
    }


    // =========================================================
    // GET TOTAL RECEIVABLE AMOUNT (DASHBOARD SUMMARY)
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalReceivableAmount() {

        log.info(
                "SERVICE - request came in getTotalReceivableAmount..."
        );

        User currentUser = currentUserService.getCurrentUser();

        List<Sale> pendingSales;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            pendingSales = saleRepo.findByPaymentStatusIn(List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
        } else {
            pendingSales = saleRepo.findByUserAndPaymentStatusIn(currentUser, List.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PARTIALLY_PAID
            ));
        }

        return pendingSales
                .stream()
                .map(sale -> {
                    BigDecimal received = salePaymentRepo.sumReceivedAmountBySale(sale);
                    return sale.getTotalAmount().subtract(received).max(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    // =========================================================
    // HELPER: BUILD SALE DETAILS
    // =========================================================

    private ResponseSalePaymentDTO.SaleDetails buildSaleDetails(
            Sale sale
    ) {

        BigDecimal receivedAmount =
                salePaymentRepo
                        .sumReceivedAmountBySale(
                                sale
                        );

        BigDecimal pendingAmount =
                sale
                        .getTotalAmount()
                        .subtract(
                                receivedAmount
                        )
                        .max(
                                BigDecimal.ZERO
                        );

        ResponseSalePaymentDTO.SaleDetails details =
                new ResponseSalePaymentDTO.SaleDetails();

        details.setPublicId(
                sale.getPublicId()
        );

        details.setSaleNumber(
                sale.getSaleNumber()
        );

        details.setCustomerName(
                sale.getCustomer() != null
                        ? sale.getCustomer().getCustomerName()
                        : null
        );

        details.setSaleDate(
                sale.getSaleDate()
        );

        details.setTotalAmount(
                sale.getTotalAmount()
        );

        details.setReceivedAmount(
                receivedAmount
        );

        details.setPendingAmount(
                pendingAmount
        );

        details.setPaymentStatus(
                sale.getPaymentStatus()
        );

        return details;
    }


    // =========================================================
    // HELPER: MAP TO RESPONSE DTO
    // =========================================================

    private ResponseSalePaymentDTO mapToResponse(
            SalePayment payment,
            Sale sale,
            BigDecimal receivedAmount
    ) {

        ResponseSalePaymentDTO response =
                modelMapper.map(
                        payment,
                        ResponseSalePaymentDTO.class
                );

        response.setSale(
                buildSaleDetails(
                        sale
                )
        );

        return response;
    }
}
