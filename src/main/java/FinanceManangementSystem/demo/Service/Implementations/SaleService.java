package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Enums.WeightUnit;
import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;

import FinanceManangementSystem.demo.Enums.DocumentType;
import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Model.Customer;
import FinanceManangementSystem.demo.Model.Sale;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestSaleDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseSaleDTO;
import FinanceManangementSystem.demo.Repository.CustomerRepository;
import FinanceManangementSystem.demo.Repository.SalePaymentRepository;
import FinanceManangementSystem.demo.Repository.SaleRepository;
import FinanceManangementSystem.demo.Service.SaleServiceInterface;
import FinanceManangementSystem.demo.Service.StockTransactionServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import FinanceManangementSystem.demo.Specification.SaleSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService
        implements SaleServiceInterface {

    private final SaleRepository saleRepo;

    private final CustomerRepository customerRepo;

    private final SalePaymentRepository salePaymentRepo;

    private final CurrentUserService currentUserService;

    private final DocumentSequenceService documentSequenceService;

    private final ModelMapper modelMapper;

    private final StockTransactionServiceInterface stockTransactionService;


    // =========================================================
    // ADD SALE
    // =========================================================

    @Override
    @Transactional
    public ResponseSaleDTO addSale(
            RequestSaleDTO dto
    ) {

        log.info(
                "SERVICE - request came in addSale..."
        );


        // -----------------------------------------------------
        // FIND ACTIVE CUSTOMER
        // -----------------------------------------------------

        log.info(
                "SERVICE - finding active customer..."
        );

        Customer customer =
                customerRepo
                        .findByPublicIdAndIsActiveTrue(
                                dto.getCustomerPublicId()
                        )
                        .orElseThrow(() -> {
                            log.info("SERVICE - active customer not found...");
                            return new ResourceNotFoundException("Active customer not found");
                        });

        // -----------------------------------------------------
        // CHECK CUSTOMER INVOICE NUMBER

        String customerInvoiceNumber =
                dto.getCustomerInvoiceNumber();

        if (customerInvoiceNumber != null) {

            customerInvoiceNumber =
                    customerInvoiceNumber.trim();

            if (!customerInvoiceNumber.isBlank()
                    && saleRepo
                    .existsByCustomerInvoiceNumberAndCustomer(
                            customerInvoiceNumber,
                            customer
                    )) {

                log.info(
                        "SERVICE - customer invoice number already exists..."
                );

                throw new InvalidRequestException(
                        "Sale with this customer invoice number already exists"
                );
            }
        }


        // -----------------------------------------------------
        // CREATE SALE
        // -----------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        Sale sale = new Sale();

        sale.setUser(currentUser);

        sale.setCustomer(
                customer
        );

        sale.setRawMaterial(
                dto.getRawMaterial().trim()
        );

        sale.setWeight(
                dto.getWeight()
        );

        sale.setUnit(
                dto.getUnit()
        );

        sale.setRatePerUnit(
                dto.getRatePerUnit()
        );

        sale.setGstPercentage(
                dto.getGstPercentage()
        );

        sale.setCustomerInvoiceNumber(
                customerInvoiceNumber
        );

        sale.setSaleDate(
                dto.getSaleDate()
        );


        // -----------------------------------------------------
        // INITIAL STATUS
        // -----------------------------------------------------

        sale.setPaymentStatus(
                PaymentStatus.PENDING
        );


        // -----------------------------------------------------
        // CALCULATE AMOUNT
        // -----------------------------------------------------

        BigDecimal amount =
                dto.getWeight()
                        .multiply(
                                dto.getRatePerUnit()
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        sale.setAmount(
                amount
        );


        // -----------------------------------------------------
        // CALCULATE GST
        // -----------------------------------------------------

        BigDecimal gstAmount =
                amount
                        .multiply(
                                dto.getGstPercentage()
                        )
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

        sale.setGstAmount(
                gstAmount
        );


        // -----------------------------------------------------
        // CALCULATE TOTAL
        // -----------------------------------------------------

        BigDecimal totalAmount =
                amount
                        .add(gstAmount)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        sale.setTotalAmount(
                totalAmount
        );


        // -----------------------------------------------------
        // GENERATE SALE NUMBER
        // -----------------------------------------------------

        log.info(
                "SERVICE - generating sale number..."
        );

        int year =
                dto.getSaleDate()
                        .getYear();

        String saleNumber =
                documentSequenceService
                        .generateDocumentNumber(
                                DocumentType.SALE,
                                year
                        );

        sale.setSaleNumber(
                saleNumber
        );


        // -----------------------------------------------------
        // SAVE SALE
        // -----------------------------------------------------

        log.info(
                "SERVICE - saving sale..."
        );

        sale =
                saleRepo.save(
                        sale
                );


        // -----------------------------------------------------
        // DEDUCT STOCK
        // -----------------------------------------------------

        log.info(
                "SERVICE - deducting sold quantity from stock pool..."
        );

        stockTransactionService.saleStockOut(
                sale.getRawMaterial(),
                sale.getUnit(),
                sale.getWeight(),
                sale.getSaleNumber()
        );


        log.info(
                "SERVICE - sale created and stock deducted successfully..."
        );


        return mapToResponse(
                sale
        );
    }


    // =========================================================
    // GET SALE BY PUBLIC ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResponseSaleDTO getSaleByPublicId(
            UUID publicId
    ) {

        log.info(
                "SERVICE - request came in getSaleByPublicId..."
        );

        User currentUser = currentUserService.getCurrentUser();

        Sale sale;

        if (currentUser.getRole() == UserRole.ADMIN) {
            sale = saleRepo.findByPublicId(publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - sale not found...");
                        return new ResourceNotFoundException("Sale not found");
                    });
        } else {
            sale = saleRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - sale not found for current user...");
                        return new ResourceNotFoundException("Sale not found");
                    });
        }

        return mapToResponse(sale);
    }


    // =========================================================
    // GET ALL SALES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ResponseSaleDTO> getAllSales(org.springframework.data.domain.Pageable pageable) {
        return getAllSales(null, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ResponseSaleDTO> getAllSales(
            UUID customerPublicId,
            PaymentStatus paymentStatus,
            LocalDate fromDate,
            LocalDate toDate,
            org.springframework.data.domain.Pageable pageable
    ) {
        log.info("SERVICE - request came in getAllSales with filters...");
        User currentUser = currentUserService.getCurrentUser();
        User filterUser = (currentUser.getRole() == UserRole.ADMIN) ? null : currentUser;

        Specification<Sale> spec = SaleSpecification.filter(
                filterUser,
                customerPublicId,
                paymentStatus,
                fromDate,
                toDate
        );

        return saleRepo.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseSaleDTO> getSalesByCustomer(UUID customerPublicId) {
        log.info("SERVICE - request came in getSalesByCustomer...");
        User currentUser = currentUserService.getCurrentUser();
        Customer customer;
        if (currentUser.getRole() == UserRole.ADMIN) {
            customer = customerRepo.findByPublicId(customerPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        } else {
            customer = customerRepo.findByUserAndPublicId(currentUser, customerPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }
        return saleRepo.findByUser(currentUser).stream()
                .filter(s -> s.getCustomer() != null && s.getCustomer().getPublicId().equals(customer.getPublicId()))
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // UPDATE SALE
    // =========================================================

    @Override
    @Transactional
    public ResponseSaleDTO updateSale(
            UUID publicId,
            RequestSaleDTO dto
    ) {

        log.info(
                "SERVICE - request came in updateSale..."
        );


        // -----------------------------------------------------
        // FIND SALE
        // -----------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        Sale sale;

        if (currentUser.getRole() == UserRole.ADMIN) {
            sale = saleRepo.findByPublicId(publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - sale not found...");
                        return new ResourceNotFoundException("Sale not found");
                    });
        } else {
            sale = saleRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> {
                        log.info("SERVICE - sale not found for current user...");
                        return new ResourceNotFoundException("Sale not found");
                    });
        }


        // -----------------------------------------------------
        // CHECK PAYMENT RESTRICTIONS
        // -----------------------------------------------------
        BigDecimal receivedAmount = salePaymentRepo.sumReceivedAmountBySale(sale);
        if (receivedAmount == null) {
            receivedAmount = BigDecimal.ZERO;
        }
        boolean hasPayments = receivedAmount.compareTo(BigDecimal.ZERO) > 0
                || salePaymentRepo.existsBySale(sale);

        // -----------------------------------------------------
        // UPDATE CUSTOMER IF CHANGED (Only if NO payment received)
        // -----------------------------------------------------
        Customer currentCustomer = sale.getCustomer();
        boolean customerChanged = !currentCustomer.getPublicId().equals(dto.getCustomerPublicId());

        if (customerChanged) {
            if (hasPayments) {
                throw new InvalidRequestException(
                        "Customer cannot be changed because payments have already been recorded against this sale invoice."
                );
            }

            log.info("SERVICE - customer changed, fetching new active customer...");
            Customer newCustomer;
            if (currentUser.getRole() == UserRole.ADMIN) {
                newCustomer = customerRepo.findByPublicIdAndIsActiveTrue(dto.getCustomerPublicId())
                        .orElseThrow(() -> new InvalidRequestException("Active customer not found"));
            } else {
                newCustomer = customerRepo.findByUserAndPublicIdAndIsActiveTrue(currentUser, dto.getCustomerPublicId())
                        .orElseThrow(() -> new InvalidRequestException("Active customer not found"));
            }

            sale.setCustomer(newCustomer);
        }

        // -----------------------------------------------------
        // RAW MATERIAL & UNIT VALIDATION
        // -----------------------------------------------------
        String oldRawMaterial = sale.getRawMaterial();
        WeightUnit oldUnit = sale.getUnit();
        BigDecimal oldWeight = sale.getWeight();

        String newRawMaterial = dto.getRawMaterial().trim();
        WeightUnit newUnit = dto.getUnit();
        BigDecimal newWeight = dto.getWeight();

        boolean materialChanged = !oldRawMaterial.equalsIgnoreCase(newRawMaterial) || oldUnit != newUnit;
        if (materialChanged && hasPayments) {
            throw new InvalidRequestException(
                    "Raw material cannot be changed because payments have already been recorded against this sale invoice."
            );
        }

        // -----------------------------------------------------
        // SYNCHRONIZE STOCK LEDGER IF WEIGHT / QUANTITY / MATERIAL CHANGED
        // -----------------------------------------------------
        boolean stockChanged = materialChanged || oldWeight.compareTo(newWeight) != 0;

        if (stockChanged) {
            stockTransactionService.updateSaleStock(
                    currentUser,
                    oldRawMaterial,
                    oldUnit,
                    oldWeight,
                    newRawMaterial,
                    newUnit,
                    newWeight,
                    sale.getSaleNumber()
            );
            sale.setRawMaterial(newRawMaterial);
            sale.setUnit(newUnit);
            sale.setWeight(newWeight);
        }

        // -----------------------------------------------------
        // CHECK CUSTOMER INVOICE NUMBER
        // -----------------------------------------------------
        String newCustomerInvoiceNumber = dto.getCustomerInvoiceNumber();
        if (newCustomerInvoiceNumber != null) {
            newCustomerInvoiceNumber = newCustomerInvoiceNumber.trim();
            if (newCustomerInvoiceNumber.isBlank()) {
                newCustomerInvoiceNumber = null;
            }
        }

        String existingCustomerInvoiceNumber = sale.getCustomerInvoiceNumber();
        boolean invoiceNumberChanged = !Objects.equals(existingCustomerInvoiceNumber, newCustomerInvoiceNumber);

        if ((invoiceNumberChanged || customerChanged)
                && newCustomerInvoiceNumber != null
                && saleRepo.existsByCustomerInvoiceNumberAndCustomer(newCustomerInvoiceNumber, sale.getCustomer())) {
            throw new InvalidRequestException("Sale with this customer reference / invoice number already exists");
        }

        sale.setCustomerInvoiceNumber(newCustomerInvoiceNumber);

        // -----------------------------------------------------
        // RE-CALCULATE AMOUNT, GST & TOTAL
        // -----------------------------------------------------
        BigDecimal amount = newWeight.multiply(dto.getRatePerUnit()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstAmount = amount.multiply(dto.getGstPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = amount.add(gstAmount).setScale(2, RoundingMode.HALF_UP);

        if (hasPayments && totalAmount.compareTo(receivedAmount) < 0) {
            throw new InvalidRequestException(
                    "Updated total amount (₹" + totalAmount + ") cannot be less than the already received amount (₹" + receivedAmount + ")."
            );
        }

        sale.setRatePerUnit(dto.getRatePerUnit());
        sale.setGstPercentage(dto.getGstPercentage());
        sale.setSaleDate(dto.getSaleDate());
        sale.setAmount(amount);
        sale.setGstAmount(gstAmount);
        sale.setTotalAmount(totalAmount);

        // Update payment status
        if (receivedAmount == null || receivedAmount.compareTo(BigDecimal.ZERO) == 0) {
            sale.setPaymentStatus(PaymentStatus.PENDING);
        } else if (receivedAmount.compareTo(totalAmount) >= 0) {
            sale.setPaymentStatus(PaymentStatus.COMPLETED);
        } else {
            sale.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        sale = saleRepo.save(sale);
        log.info("SERVICE - sale updated successfully: {}", sale.getSaleNumber());

        return mapToResponse(sale);
    }


    // =========================================================
    // DELETE SALE
    // =========================================================

    @Override
    @Transactional
    public void deleteSale(UUID publicId) {
        log.info("SERVICE - request came in deleteSale: {}", publicId);

        User currentUser = currentUserService.getCurrentUser();
        Sale sale;

        if (currentUser.getRole() == UserRole.ADMIN) {
            sale = saleRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
        } else {
            sale = saleRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
        }

        // -----------------------------------------------------
        // VERIFY ZERO PAYMENTS
        // -----------------------------------------------------
        BigDecimal receivedAmount = salePaymentRepo.sumReceivedAmountBySale(sale);
        boolean hasPayments = (receivedAmount != null && receivedAmount.compareTo(BigDecimal.ZERO) > 0)
                || salePaymentRepo.existsBySale(sale);

        if (hasPayments) {
            throw new InvalidRequestException(
                    "Cannot delete sale invoice #" + sale.getSaleNumber() +
                            ": Payments have already been received against this sale invoice. " +
                            "Please delete or reverse all associated payments in Customer Payments before deleting this sale invoice."
            );
        }

        // -----------------------------------------------------
        // REVERT STOCK (Return sold goods back to inventory)
        // -----------------------------------------------------
        stockTransactionService.revertSaleStock(
                currentUser,
                sale.getRawMaterial(),
                sale.getUnit(),
                sale.getWeight(),
                sale.getSaleNumber()
        );

        // -----------------------------------------------------
        // DELETE ENTITY
        // -----------------------------------------------------
        saleRepo.delete(sale);
        log.info("SERVICE - sale {} deleted successfully", sale.getSaleNumber());
    }


    // =========================================================
    // ENTITY → RESPONSE DTO
    // =========================================================

    private ResponseSaleDTO mapToResponse(
            Sale sale
    ) {

        log.info(
                "SERVICE - mapping sale to response DTO..."
        );

        ResponseSaleDTO response =
                modelMapper.map(
                        sale,
                        ResponseSaleDTO.class
                );

        Customer customer =
                sale.getCustomer();

        if (customer != null) {

            ResponseSaleDTO.CustomerDetails customerDetails =
                    new ResponseSaleDTO.CustomerDetails();

            customerDetails.setPublicId(
                    customer.getPublicId()
            );

            customerDetails.setCustomerName(
                    customer.getCustomerName()
            );

            customerDetails.setMobileNumber(
                    customer.getMobileNumber()
            );

            customerDetails.setEmail(
                    customer.getEmail()
            );

            customerDetails.setGstNumber(
                    customer.getGstNumber()
            );

            response.setCustomer(
                    customerDetails
            );
        }

        return response;
    }
}
