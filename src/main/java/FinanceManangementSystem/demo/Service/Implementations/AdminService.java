package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Enums.PaymentStatus;
import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Exceptions.DuplicateResourceException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;
import FinanceManangementSystem.demo.Model.*;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUpdateUserDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUserDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.*;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.AdminDashboardStatsDTO.PlatformActivityDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.AdminDashboardStatsDTO.TopClientDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.Client360DTO.ClientPartnerDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.Client360DTO.ClientRecentPaymentDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.Client360DTO.ClientRecentPurchaseDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.Client360DTO.ClientRecentSaleDTO;
import FinanceManangementSystem.demo.Repository.*;
import FinanceManangementSystem.demo.Service.AdminServiceInterface;
import FinanceManangementSystem.demo.Service.PurchasePaymentServiceInterface;
import FinanceManangementSystem.demo.Service.SalePaymentServiceInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class AdminService implements AdminServiceInterface {

    private final UserRepository userRepo;
    private final ModelMapper modelMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepo;
    private final SupplierRepository supplierRepo;
    private final StockRepository stockRepo;
    private final SaleRepository saleRepo;
    private final PurchaseRepository purchaseRepo;
    private final ExpenseRepository expenseRepo;
    private final SalePaymentRepository salePaymentRepo;
    private final PurchasePaymentRepository purchasePaymentRepo;
    private final PartnerRepository partnerRepo;
    private final PartnerProfitWithdrawalRepository partnerProfitWithdrawalRepo;
    private final PurchasePaymentServiceInterface purchasePaymentService;
    private final SalePaymentServiceInterface salePaymentService;


    @Transactional
    @Override
    public ResponseUserDTO registration(RequestUserDTO dto) {
        log.info("SERVICE - request came in registration...");

        Optional<String> name = userRepo.findByEmailOrContact(dto.getEmail(),dto.getMobileNumber());

        if(name.isPresent()){
            throw new DuplicateResourceException("User already exists...");
        }

        User user = modelMapper.map(dto,User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (dto.getRole() == null || dto.getRole() == UserRole.CLIENT) {
            user.setViewablePassword(dto.getPassword());
        }

        if (dto.getUserAddress() != null) {
            UserAddress address = modelMapper.map(dto.getUserAddress(), UserAddress.class);

            address.setUser(user);

            user.setAddress(address);
        }

        user = userRepo.save(user);

        log.info("SERVICE - registered successfully...");

        ResponseUserDTO response = modelMapper.map(user,ResponseUserDTO.class);
        if (user.getAddress() != null) {
            response.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
        }
        if (user.getRole() == UserRole.CLIENT) {
            response.setViewablePassword(user.getViewablePassword());
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseUserDTO> listAllUsers() {
        log.info("SERVICE - request came in listAllUsers...");

        // Filter explicitly by UserRole.CLIENT to exclude ADMIN accounts from the clients list
        List<User> users = userRepo.findByRoleOrderByCreatedAtDesc(UserRole.CLIENT);

        log.info("SERVICE - clients fetched successfully, count: {}", users.size());

        return users.stream()
                .map(user -> {
                    ResponseUserDTO dto = modelMapper.map(user, ResponseUserDTO.class);
                    if (user.getAddress() != null) {
                        dto.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
                    }
                    if (user.getRole() == UserRole.CLIENT) {
                        dto.setViewablePassword(user.getViewablePassword());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseUserDTO getUserByPublicId(UUID publicId) {
        log.info("SERVICE - request came in getUserByPublicId for publicId: {}", publicId);
        User user = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with publicId: " + publicId));
        ResponseUserDTO response = modelMapper.map(user, ResponseUserDTO.class);
        if (user.getAddress() != null) {
            response.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
        }
        if (user.getRole() == UserRole.CLIENT) {
            response.setViewablePassword(user.getViewablePassword());
        }
        return response;
    }

    @Transactional
    @Override
    public ResponseUserDTO updateUser(UUID publicId, RequestUpdateUserDTO dto) {
        log.info("SERVICE - request came in updateUser for publicId: {}", publicId);

        User user = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with publicId: " + publicId));

        // Check for email or mobile conflict with other users
        Optional<String> duplicate = userRepo.findByEmailOrContactAndNotPublicId(dto.getEmail(), dto.getMobileNumber(), publicId);
        if (duplicate.isPresent()) {
            throw new DuplicateResourceException("Another user with this email or mobile number already exists.");
        }

        user.setOwnerName(dto.getOwnerName());
        user.setEmail(dto.getEmail());
        user.setMobileNumber(dto.getMobileNumber());

        if (dto.getNewPassword() != null && !dto.getNewPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getNewPassword().trim()));
            if (user.getRole() == UserRole.CLIENT) {
                user.setViewablePassword(dto.getNewPassword().trim());
            }
            log.info("SERVICE - password reset for client: {} by Admin", user.getUsername());
        }

        if (dto.getUserAddress() != null) {
            UserAddress address = user.getAddress();
            if (address == null) {
                address = new UserAddress();
                address.setUser(user);
                user.setAddress(address);
            }
            address.setHouseNo(dto.getUserAddress().getHouseNo());
            address.setSocietyName(dto.getUserAddress().getSocietyName());
            address.setArea(dto.getUserAddress().getArea());
            address.setCity(dto.getUserAddress().getCity());
            address.setPincode(dto.getUserAddress().getPincode());
            address.setState(dto.getUserAddress().getState());
            address.setCountry(dto.getUserAddress().getCountry() != null && !dto.getUserAddress().getCountry().isBlank()
                    ? dto.getUserAddress().getCountry() : "India");
        }

        user = userRepo.save(user);

        log.info("SERVICE - user updated successfully...");

        ResponseUserDTO response = modelMapper.map(user, ResponseUserDTO.class);
        if (user.getAddress() != null) {
            response.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
        }
        if (user.getRole() == UserRole.CLIENT) {
            response.setViewablePassword(user.getViewablePassword());
        }
        return response;
    }

    @Transactional
    @Override
    public void deactivateUser(UUID publicId) {
        log.info("SERVICE - request came in deactivateUser for publicId: {}", publicId);
        User user = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with publicId: " + publicId));
        user.setEnabled(false);
        userRepo.save(user);
        log.info("SERVICE - user deactivated successfully...");
    }

    @Transactional
    @Override
    public void reactivateUser(UUID publicId) {
        log.info("SERVICE - request came in reactivateUser for publicId: {}", publicId);
        User user = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with publicId: " + publicId));
        user.setEnabled(true);
        userRepo.save(user);
        log.info("SERVICE - user reactivated successfully...");
    }

    @Transactional(readOnly = true)
    @Override
    public AdminDashboardStatsDTO getAdminDashboardStats() {
        log.info("SERVICE - request came in getAdminDashboardStats...");
        AdminDashboardStatsDTO dto = new AdminDashboardStatsDTO();

        List<User> clients = userRepo.findByRoleOrderByCreatedAtDesc(UserRole.CLIENT);
        long totalClients = clients.size();
        long activeClients = clients.stream().filter(u -> Boolean.TRUE.equals(u.getEnabled())).count();
        long inactiveClients = totalClients - activeClients;

        dto.setTotalClients(totalClients);
        dto.setActiveClients(activeClients);
        dto.setInactiveClients(inactiveClients);

        dto.setTotalCustomers(customerRepo.count());
        dto.setTotalSuppliers(supplierRepo.count());
        dto.setTotalStockItems(stockRepo.count());

        List<Sale> allSales = saleRepo.findAll();
        BigDecimal grossSales = allSales.stream()
                .map(s -> s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalGrossSales(grossSales);

        List<Purchase> allPurchases = purchaseRepo.findAll();
        BigDecimal grossPurchases = allPurchases.stream()
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalGrossPurchases(grossPurchases);

        BigDecimal totalReceived = salePaymentRepo.sumTotalReceived();
        dto.setTotalPaymentsReceived(totalReceived != null ? totalReceived : BigDecimal.ZERO);

        BigDecimal totalPaid = purchasePaymentRepo.sumTotalPaid();
        dto.setTotalPaymentsMade(totalPaid != null ? totalPaid : BigDecimal.ZERO);

        BigDecimal totalExp = expenseRepo.sumTotalExpensesByDateRange(null, null);
        dto.setTotalExpenses(totalExp != null ? totalExp : BigDecimal.ZERO);

        BigDecimal outstanding = purchasePaymentService.getTotalOutstandingAmount();
        BigDecimal receivable = salePaymentService.getTotalReceivableAmount();
        dto.setTotalPayables(outstanding != null ? outstanding : BigDecimal.ZERO);
        dto.setTotalReceivables(receivable != null ? receivable : BigDecimal.ZERO);

        BigDecimal netWorkingCapital = dto.getTotalReceivables().subtract(dto.getTotalPayables());
        dto.setNetWorkingCapital(netWorkingCapital);
        if (netWorkingCapital.compareTo(BigDecimal.ZERO) > 0) {
            dto.setCashFlowStatus("SURPLUS");
        } else if (netWorkingCapital.compareTo(BigDecimal.ZERO) < 0) {
            dto.setCashFlowStatus("DEFICIT");
        } else {
            dto.setCashFlowStatus("BALANCED");
        }

        // Top clients leaderboard
        List<TopClientDTO> topClients = new ArrayList<>();
        for (User client : clients) {
            List<Sale> clientSales = saleRepo.findByUser(client);
            BigDecimal vol = clientSales.stream()
                    .map(s -> s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long custCount = customerRepo.findByUser(client).size();
            topClients.add(new TopClientDTO(
                    client.getPublicId(),
                    client.getOwnerName(),
                    client.getUsername(),
                    client.getEmail(),
                    client.getMobileNumber(),
                    clientSales.size(),
                    vol,
                    custCount,
                    Boolean.TRUE.equals(client.getEnabled())
            ));
        }
        topClients.sort(Comparator.comparing(TopClientDTO::getTotalSalesVolume, Comparator.reverseOrder()));
        if (topClients.size() > 10) {
            topClients = topClients.subList(0, 10);
        }
        dto.setTopClients(topClients);

        // Recent activity feed across platform
        List<PlatformActivityDTO> activities = new ArrayList<>();
        List<Sale> recentSales = saleRepo.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "saleDate", "id"))).getContent();
        for (Sale s : recentSales) {
            String clientName = s.getUser() != null ? s.getUser().getOwnerName() : "Client";
            String party = s.getCustomer() != null ? s.getCustomer().getCustomerName() : "Customer";
            String status = s.getPaymentStatus() != null ? s.getPaymentStatus().name() : "PENDING";
            activities.add(new PlatformActivityDTO("SALE_INVOICE", clientName, s.getSaleNumber(), party, s.getTotalAmount(), s.getSaleDate(), status));
        }

        List<Purchase> recentPurchases = purchaseRepo.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "purchaseDate", "id"))).getContent();
        for (Purchase p : recentPurchases) {
            String clientName = p.getUser() != null ? p.getUser().getOwnerName() : "Client";
            String party = p.getSupplier() != null ? p.getSupplier().getSupplierName() : "Supplier";
            String status = p.getPaymentStatus() != null ? p.getPaymentStatus().name() : "PENDING";
            activities.add(new PlatformActivityDTO("PURCHASE_BILL", clientName, p.getPurchaseNumber(), party, p.getTotalAmount(), p.getPurchaseDate(), status));
        }

        activities.sort(Comparator.comparing(PlatformActivityDTO::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        if (activities.size() > 10) {
            activities = activities.subList(0, 10);
        }
        dto.setRecentActivities(activities);

        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public Client360DTO getClient360(UUID publicId) {
        log.info("SERVICE - request came in getClient360 for publicId: {}", publicId);
        User client = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with publicId: " + publicId));

        Client360DTO dto = new Client360DTO();

        // 1. Account Profile
        dto.setPublicId(client.getPublicId());
        dto.setOwnerName(client.getOwnerName());
        dto.setUsername(client.getUsername());
        dto.setEmail(client.getEmail());
        dto.setMobileNumber(client.getMobileNumber());
        dto.setRole(client.getRole() != null ? client.getRole().name() : "CLIENT");
        dto.setEnabled(client.getEnabled());
        dto.setAccountNonLocked(client.getAccountNonLocked());
        dto.setFailedLoginAttempts(client.getFailedLoginAttempts());
        dto.setLockTime(client.getLockTime());
        dto.setCreatedAt(client.getCreatedAt());
        if (client.getAddress() != null) {
            dto.setAddress(modelMapper.map(client.getAddress(), ResponseUserAddressDTO.class));
        }
        if (client.getRole() == UserRole.CLIENT) {
            dto.setViewablePassword(client.getViewablePassword());
        }

        // 2. Financial Metrics Rollup
        BigDecimal openingBalance = client.getOpeningBalance() != null ? client.getOpeningBalance() : BigDecimal.ZERO;
        dto.setOpeningBalance(openingBalance);

        List<Sale> clientSales = saleRepo.findByUser(client);
        BigDecimal totalGrossSales = clientSales.stream()
                .map(s -> s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalGrossSales(totalGrossSales);
        dto.setTotalSalesCount(clientSales.size());

        List<Purchase> clientPurchases = purchaseRepo.findByUser(client);
        BigDecimal totalGrossPurchases = clientPurchases.stream()
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalGrossPurchases(totalGrossPurchases);
        dto.setTotalPurchasesCount(clientPurchases.size());

        BigDecimal totalReceived = salePaymentRepo.sumTotalReceivedByUser(client);
        if (totalReceived == null) totalReceived = BigDecimal.ZERO;
        dto.setTotalPaymentsReceived(totalReceived);

        BigDecimal totalPaid = purchasePaymentRepo.sumTotalPaidByUser(client);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        dto.setTotalPaymentsMade(totalPaid);

        BigDecimal totalExpenses = expenseRepo.sumTotalExpensesByUserAndDateRange(client, null, null);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        dto.setTotalExpenses(totalExpenses);

        BigDecimal totalWithdrawals = partnerProfitWithdrawalRepo.sumWithdrawnByUser(client);
        if (totalWithdrawals == null) totalWithdrawals = BigDecimal.ZERO;
        dto.setTotalWithdrawals(totalWithdrawals);

        BigDecimal netProfit = totalReceived.subtract(totalPaid).subtract(totalExpenses);
        BigDecimal totalBalance = openingBalance.add(totalReceived).subtract(totalPaid).subtract(totalExpenses).subtract(totalWithdrawals);
        dto.setNetProfit(netProfit);
        dto.setTotalBalance(totalBalance);

        // Receivables & Payables for this client
        BigDecimal totalReceivables = saleRepo.findByUserAndPaymentStatusIn(client, List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID))
                .stream()
                .map(s -> {
                    BigDecimal rec = salePaymentRepo.sumReceivedAmountBySale(s);
                    return s.getTotalAmount().subtract(rec != null ? rec : BigDecimal.ZERO).max(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalReceivables(totalReceivables);

        BigDecimal totalPayables = purchaseRepo.findByUserAndPaymentStatusIn(client, List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID))
                .stream()
                .map(p -> {
                    BigDecimal paid = purchasePaymentRepo.sumPaidAmountByPurchase(p);
                    return p.getTotalAmount().subtract(paid != null ? paid : BigDecimal.ZERO).max(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalPayables(totalPayables);
        dto.setNetWorkingCapital(totalReceivables.subtract(totalPayables));

        // 3. Entity Counters
        dto.setTotalCustomers(customerRepo.findByUser(client).size());
        dto.setTotalSuppliers(supplierRepo.findByUser(client).size());
        dto.setTotalStockItems(stockRepo.findByUser(client).size());

        List<Partner> partners = partnerRepo.findByUser(client);
        dto.setTotalPartners(partners.size());

        // 4. Partner Structure
        List<ClientPartnerDTO> partnerDTOs = partners.stream().map(p -> {
            BigDecimal withdrawn = partnerProfitWithdrawalRepo.sumWithdrawnByPartnerAndUser(p, client);
            return new ClientPartnerDTO(
                    p.getPublicId(),
                    p.getPartnerName(),
                    p.getMobileNumber(),
                    p.getEmail(),
                    p.getSharePercentage(),
                    withdrawn != null ? withdrawn : BigDecimal.ZERO,
                    p.getIsActive()
            );
        }).toList();
        dto.setPartners(partnerDTOs);

        // 5. Recent Transaction Streams
        List<Sale> recentSales = saleRepo.findByUser(client, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "saleDate", "id"))).getContent();
        dto.setRecentSales(recentSales.stream().map(s -> new ClientRecentSaleDTO(
                s.getPublicId(),
                s.getSaleNumber(),
                s.getCustomer() != null ? s.getCustomer().getCustomerName() : "Customer",
                s.getTotalAmount(),
                s.getPaymentStatus() != null ? s.getPaymentStatus().name() : "PENDING",
                s.getSaleDate()
        )).toList());

        List<Purchase> recentPurchases = purchaseRepo.findByUser(client, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "purchaseDate", "id"))).getContent();
        dto.setRecentPurchases(recentPurchases.stream().map(p -> new ClientRecentPurchaseDTO(
                p.getPublicId(),
                p.getPurchaseNumber(),
                p.getSupplier() != null ? p.getSupplier().getSupplierName() : "Supplier",
                p.getTotalAmount(),
                p.getPaymentStatus() != null ? p.getPaymentStatus().name() : "PENDING",
                p.getPurchaseDate()
        )).toList());

        List<ClientRecentPaymentDTO> recentPayments = new ArrayList<>();
        List<SalePayment> spList = salePaymentRepo.findAll().stream()
                .filter(sp -> client.equals(sp.getUser()))
                .sorted(Comparator.comparing(SalePayment::getPaymentDate, Comparator.reverseOrder()))
                .limit(5)
                .toList();
        for (SalePayment sp : spList) {
            String party = (sp.getSale() != null && sp.getSale().getCustomer() != null) ? sp.getSale().getCustomer().getCustomerName() : "Customer";
            recentPayments.add(new ClientRecentPaymentDTO(
                    sp.getPublicId(),
                    "CUSTOMER_RECEIPT",
                    sp.getReferenceNumber(),
                    party,
                    sp.getAmountReceived(),
                    sp.getPaymentMode() != null ? sp.getPaymentMode().name() : "CASH",
                    sp.getPaymentDate()
            ));
        }
        dto.setRecentPayments(recentPayments);

        return dto;
    }

    @Transactional
    @Override
    public void unlockUser(UUID publicId) {
        log.info("SERVICE - request came in unlockUser for publicId: {}", publicId);
        User user = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with publicId: " + publicId));
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        user.setAccountNonLocked(true);
        userRepo.save(user);
        log.info("SERVICE - user unlocked successfully...");
    }
}
