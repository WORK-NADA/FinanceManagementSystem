package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;

import FinanceManangementSystem.demo.Model.Partner;
import FinanceManangementSystem.demo.Model.PartnerProfitShare;
import FinanceManangementSystem.demo.Model.ProfitDistribution;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestProfitDistributionDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseProfitDistributionDTO;
import FinanceManangementSystem.demo.Repository.PartnerProfitShareRepository;
import FinanceManangementSystem.demo.Repository.PartnerRepository;
import FinanceManangementSystem.demo.Repository.ProfitDistributionRepository;
import FinanceManangementSystem.demo.Repository.PurchaseRepository;
import FinanceManangementSystem.demo.Repository.SaleRepository;
import FinanceManangementSystem.demo.Service.ExpenseServiceInterface;
import FinanceManangementSystem.demo.Service.ProfitDistributionServiceInterface;
import FinanceManangementSystem.demo.Model.PartnerProfitWithdrawal;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestProfitWithdrawalDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.LiveProfitSharingOverviewDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseProfitWithdrawalDTO;
import FinanceManangementSystem.demo.Repository.ExpenseRepository;
import FinanceManangementSystem.demo.Repository.PartnerProfitWithdrawalRepository;
import FinanceManangementSystem.demo.Repository.PurchasePaymentRepository;
import FinanceManangementSystem.demo.Repository.SalePaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitDistributionService implements ProfitDistributionServiceInterface {

    private final SaleRepository saleRepo;

    private final PurchaseRepository purchaseRepo;

    private final SalePaymentRepository salePaymentRepo;

    private final PurchasePaymentRepository purchasePaymentRepo;

    private final ExpenseServiceInterface expenseService;

    private final PartnerRepository partnerRepo;

    private final ProfitDistributionRepository distributionRepo;

    private final PartnerProfitShareRepository shareRepo;

    private final PartnerProfitWithdrawalRepository withdrawalRepo;

    private final ExpenseRepository expenseRepo;

    private final CurrentUserService currentUserService;

    private final ModelMapper modelMapper;


    @Override
    @Transactional(readOnly = true)
    public ResponseProfitDistributionDTO previewDistribution(RequestProfitDistributionDTO dto) {
        log.info("SERVICE - request came in previewDistribution...");

        LocalDate from = dto.getFromDate();
        LocalDate to = dto.getToDate();

        if (from.isAfter(to)) {
            throw new InvalidRequestException("fromDate must be before or equal to toDate");
        }

        User currentUser = currentUserService.getCurrentUser();

        Optional<ProfitDistribution> existingOpt = distributionRepo.findByUserAndFromDateAndToDate(currentUser, from, to);
        boolean isRecalculation = existingOpt.isPresent();

        List<Partner> activePartners = partnerRepo.findByUserAndIsActiveTrue(currentUser);

        if (activePartners.isEmpty()) {
            throw new InvalidRequestException("No active partners to distribute profit");
        }

        BigDecimal activeSum = partnerRepo.sumActiveSharePercentage(currentUser);
        if (activeSum == null) activeSum = BigDecimal.ZERO;

        if (activeSum.compareTo(new BigDecimal("100.00")) != 0) {
            throw new InvalidRequestException("Active partner shares must total exactly 100% before distribution");
        }

        // Compute totals based on actual customer money received and supplier money paid
        BigDecimal totalRevenue = salePaymentRepo.sumTotalReceivedByUserAndDateRange(currentUser, from, to);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal totalPurchaseCost = purchasePaymentRepo.sumTotalPaidByUserAndDateRange(currentUser, from, to);
        if (totalPurchaseCost == null) totalPurchaseCost = BigDecimal.ZERO;

        BigDecimal totalExpenses = expenseService.getTotalExpenses(currentUser, from, to);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal netProfit = totalRevenue.subtract(totalPurchaseCost).subtract(totalExpenses);

        List<Partner> sortedPartners = new ArrayList<>(activePartners);
        sortedPartners.sort(Comparator.comparing(p -> p.getPublicId().toString()));

        List<ResponseProfitDistributionDTO.PartnerShareDetails> shareDetails = new ArrayList<>();
        BigDecimal sumRounded = BigDecimal.ZERO;

        for (Partner partner : sortedPartners) {
            BigDecimal rawShare = netProfit.multiply(partner.getSharePercentage())
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal rounded = rawShare.setScale(2, RoundingMode.HALF_UP);

            sumRounded = sumRounded.add(rounded);

            ResponseProfitDistributionDTO.PartnerShareDetails pd = new ResponseProfitDistributionDTO.PartnerShareDetails();
            pd.setPartnerPublicId(partner.getPublicId());
            pd.setPartnerName(partner.getPartnerName());
            pd.setSharePercentageAtDistribution(partner.getSharePercentage());
            pd.setShareAmount(rounded);

            shareDetails.add(pd);
        }

        BigDecimal remainder = netProfit.subtract(sumRounded);
        if (remainder.compareTo(BigDecimal.ZERO) != 0 && !shareDetails.isEmpty()) {
            ResponseProfitDistributionDTO.PartnerShareDetails lastPd = shareDetails.get(shareDetails.size() - 1);
            lastPd.setShareAmount(lastPd.getShareAmount().add(remainder));
        }

        ResponseProfitDistributionDTO resp = new ResponseProfitDistributionDTO();
        resp.setPublicId(existingOpt.map(ProfitDistribution::getPublicId).orElseGet(UUID::randomUUID));
        resp.setFromDate(from);
        resp.setToDate(to);
        resp.setTotalRevenue(totalRevenue);
        resp.setTotalPurchaseCost(totalPurchaseCost);
        resp.setTotalExpenses(totalExpenses);
        resp.setNetProfit(netProfit);
        resp.setCreatedAt(existingOpt.map(ProfitDistribution::getCreatedAt).orElseGet(java.time.LocalDateTime::now));
        resp.setUpdatedAt(existingOpt.map(ProfitDistribution::getUpdatedAt).orElse(null));
        resp.setIsRecalculation(isRecalculation);
        resp.setShares(shareDetails);

        return resp;
    }

    @Override
    @Transactional
    public ResponseProfitDistributionDTO calculateAndDistribute(RequestProfitDistributionDTO dto) {
        log.info("SERVICE - request came in calculateAndDistribute...");

        LocalDate from = dto.getFromDate();
        LocalDate to = dto.getToDate();

        if (from.isAfter(to)) {
            throw new InvalidRequestException("fromDate must be before or equal to toDate");
        }

        User currentUser = currentUserService.getCurrentUser();

        Optional<ProfitDistribution> existingOpt = distributionRepo.findByUserAndFromDateAndToDate(currentUser, from, to);
        boolean isRecalculation = existingOpt.isPresent();

        List<Partner> activePartners = partnerRepo.findByUserAndIsActiveTrue(currentUser);

        if (activePartners.isEmpty()) {
            throw new InvalidRequestException("No active partners to distribute profit");
        }

        BigDecimal activeSum = partnerRepo.sumActiveSharePercentage(currentUser);
        if (activeSum == null) activeSum = BigDecimal.ZERO;

        if (activeSum.compareTo(new BigDecimal("100.00")) != 0) {
            throw new InvalidRequestException("Active partner shares must total exactly 100% before distribution");
        }

        // Compute totals based on actual customer money received and supplier money paid
        BigDecimal totalRevenue = salePaymentRepo.sumTotalReceivedByUserAndDateRange(currentUser, from, to);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal totalPurchaseCost = purchasePaymentRepo.sumTotalPaidByUserAndDateRange(currentUser, from, to);
        if (totalPurchaseCost == null) totalPurchaseCost = BigDecimal.ZERO;

        BigDecimal totalExpenses = expenseService.getTotalExpenses(currentUser, from, to);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal netProfit = totalRevenue.subtract(totalPurchaseCost).subtract(totalExpenses);

        ProfitDistribution dist;
        if (isRecalculation) {
            dist = existingOpt.get();
            dist.setTotalRevenue(totalRevenue);
            dist.setTotalPurchaseCost(totalPurchaseCost);
            dist.setTotalExpenses(totalExpenses);
            dist.setNetProfit(netProfit);
            dist.setUpdatedAt(java.time.LocalDateTime.now());
            dist = distributionRepo.save(dist);

            // Clean up old shares to prevent duplicate allocations
            List<PartnerProfitShare> oldShares = shareRepo.findByDistribution(dist);
            if (!oldShares.isEmpty()) {
                shareRepo.deleteAll(oldShares);
                shareRepo.flush();
            }
        } else {
            dist = new ProfitDistribution();
            dist.setUser(currentUser);
            dist.setFromDate(from);
            dist.setToDate(to);
            dist.setTotalRevenue(totalRevenue);
            dist.setTotalPurchaseCost(totalPurchaseCost);
            dist.setTotalExpenses(totalExpenses);
            dist.setNetProfit(netProfit);
            dist.setUpdatedAt(java.time.LocalDateTime.now());
            dist = distributionRepo.save(dist);
        }

        // Sort partners deterministically by publicId string
        List<Partner> sortedPartners = new ArrayList<>(activePartners);
        sortedPartners.sort(Comparator.comparing(p -> p.getPublicId().toString()));

        List<ResponseProfitDistributionDTO.PartnerShareDetails> shareDetails = new ArrayList<>();

        BigDecimal sumRounded = BigDecimal.ZERO;

        List<PartnerProfitShare> toSave = new ArrayList<>();

        for (Partner partner : sortedPartners) {

            BigDecimal rawShare = netProfit.multiply(partner.getSharePercentage())
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            BigDecimal rounded = rawShare.setScale(2, RoundingMode.HALF_UP);

            PartnerProfitShare s = new PartnerProfitShare();
            s.setDistribution(dist);
            s.setPartner(partner);
            s.setSharePercentageAtDistribution(partner.getSharePercentage());
            s.setShareAmount(rounded);

            toSave.add(s);

            sumRounded = sumRounded.add(rounded);

            ResponseProfitDistributionDTO.PartnerShareDetails pd = new ResponseProfitDistributionDTO.PartnerShareDetails();
            pd.setPartnerPublicId(partner.getPublicId());
            pd.setPartnerName(partner.getPartnerName());
            pd.setSharePercentageAtDistribution(partner.getSharePercentage());
            pd.setShareAmount(rounded);

            shareDetails.add(pd);
        }

        BigDecimal remainder = netProfit.subtract(sumRounded);

        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            // Add remainder to last partner
            PartnerProfitShare last = toSave.get(toSave.size() - 1);
            last.setShareAmount(last.getShareAmount().add(remainder));

            // Update response shareDetails last element
            ResponseProfitDistributionDTO.PartnerShareDetails lastPd = shareDetails.get(shareDetails.size() - 1);
            lastPd.setShareAmount(lastPd.getShareAmount().add(remainder));
        }

        // Save shares
        shareRepo.saveAll(toSave);

        ResponseProfitDistributionDTO resp = new ResponseProfitDistributionDTO();
        resp.setPublicId(dist.getPublicId());
        resp.setFromDate(dist.getFromDate());
        resp.setToDate(dist.getToDate());
        resp.setTotalRevenue(dist.getTotalRevenue());
        resp.setTotalPurchaseCost(dist.getTotalPurchaseCost());
        resp.setTotalExpenses(dist.getTotalExpenses());
        resp.setNetProfit(dist.getNetProfit());
        resp.setCreatedAt(dist.getCreatedAt());
        resp.setUpdatedAt(dist.getUpdatedAt());
        resp.setIsRecalculation(isRecalculation);
        resp.setShares(shareDetails);

        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseProfitDistributionDTO getDistributionByPublicId(UUID publicId) {
        User currentUser = currentUserService.getCurrentUser();

        ProfitDistribution dist;
        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            dist = distributionRepo.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Distribution not found"));
        } else {
            dist = distributionRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Distribution not found"));
        }

        ResponseProfitDistributionDTO resp = modelMapper.map(dist, ResponseProfitDistributionDTO.class);
        resp.setUpdatedAt(dist.getUpdatedAt());
        resp.setIsRecalculation(dist.getUpdatedAt() != null);

        List<PartnerProfitShare> shares = shareRepo.findByDistribution(dist);

        List<ResponseProfitDistributionDTO.PartnerShareDetails> details = shares.stream().map(s -> {
            ResponseProfitDistributionDTO.PartnerShareDetails pd = new ResponseProfitDistributionDTO.PartnerShareDetails();
            pd.setPartnerPublicId(s.getPartner().getPublicId());
            pd.setPartnerName(s.getPartner().getPartnerName());
            pd.setSharePercentageAtDistribution(s.getSharePercentageAtDistribution());
            pd.setShareAmount(s.getShareAmount());
            return pd;
        }).collect(Collectors.toList());

        resp.setShares(details);

        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseProfitDistributionDTO> getAllDistributions() {
        User currentUser = currentUserService.getCurrentUser();

        List<ProfitDistribution> dists;
        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            dists = distributionRepo.findAllOrderByLatestActivity();
        } else {
            dists = distributionRepo.findByUserOrderByLatestActivity(currentUser);
        }

        return dists.stream().map(dist -> {
            ResponseProfitDistributionDTO resp = modelMapper.map(dist, ResponseProfitDistributionDTO.class);
            resp.setUpdatedAt(dist.getUpdatedAt());
            resp.setIsRecalculation(dist.getUpdatedAt() != null);
            List<PartnerProfitShare> shares = shareRepo.findByDistribution(dist);
            List<ResponseProfitDistributionDTO.PartnerShareDetails> details = shares.stream().map(s -> {
                ResponseProfitDistributionDTO.PartnerShareDetails pd = new ResponseProfitDistributionDTO.PartnerShareDetails();
                pd.setPartnerPublicId(s.getPartner().getPublicId());
                pd.setPartnerName(s.getPartner().getPartnerName());
                pd.setSharePercentageAtDistribution(s.getSharePercentageAtDistribution());
                pd.setShareAmount(s.getShareAmount());
                return pd;
            }).collect(Collectors.toList());
            resp.setShares(details);
            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseProfitDistributionDTO.PartnerShareDetails> getShareHistoryByPartner(UUID partnerPublicId) {
        User currentUser = currentUserService.getCurrentUser();
        List<PartnerProfitShare> shares;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            shares = shareRepo.findByPartner_PublicIdOrderByCreatedAtDesc(partnerPublicId);
        } else {
            shares = shareRepo.findByPartner_PublicIdAndDistribution_UserOrderByCreatedAtDesc(partnerPublicId, currentUser);
        }

        return shares.stream()
                .map(s -> {
                    ResponseProfitDistributionDTO.PartnerShareDetails pd = new ResponseProfitDistributionDTO.PartnerShareDetails();
                    pd.setPartnerPublicId(s.getPartner().getPublicId());
                    pd.setPartnerName(s.getPartner().getPartnerName());
                    pd.setSharePercentageAtDistribution(s.getSharePercentageAtDistribution());
                    pd.setShareAmount(s.getShareAmount());
                    return pd;
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseProfitDistributionDTO getLatestDistribution() {
        User currentUser = currentUserService.getCurrentUser();
        ProfitDistribution dist;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            List<ProfitDistribution> list = distributionRepo.findAllOrderByLatestActivity();
            dist = list.isEmpty() ? null : list.get(0);
        } else {
            List<ProfitDistribution> list = distributionRepo.findByUserOrderByLatestActivity(currentUser);
            dist = list.isEmpty() ? null : list.get(0);
        }

        if (dist == null) {
            return null;
        }

        return getDistributionByPublicId(dist.getPublicId());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getLifetimeEarningsByPartner(UUID partnerPublicId) {
        User currentUser = currentUserService.getCurrentUser();
        BigDecimal sum;

        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            sum = shareRepo.sumLifetimeEarningsByPartner(partnerPublicId);
        } else {
            sum = shareRepo.sumLifetimeEarningsByPartnerAndUser(partnerPublicId, currentUser);
        }
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    @Transactional(readOnly = true)
    public LiveProfitSharingOverviewDTO getLiveProfitOverview() {
        log.info("SERVICE - request came in getLiveProfitOverview...");
        User currentUser = currentUserService.getCurrentUser();

        BigDecimal totalReceived = salePaymentRepo.sumTotalReceivedByUser(currentUser);
        if (totalReceived == null) totalReceived = BigDecimal.ZERO;

        BigDecimal totalPaid = purchasePaymentRepo.sumTotalPaidByUser(currentUser);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        BigDecimal totalExpenses = expenseRepo.sumTotalExpensesByUserAndDateRange(currentUser, null, null);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal netProfit = totalReceived.subtract(totalPaid).subtract(totalExpenses);

        List<Partner> partners = partnerRepo.findByUser(currentUser);
        partners.sort(Comparator.comparing(Partner::getIsActive, Comparator.reverseOrder())
                .thenComparing(Partner::getPartnerName, String.CASE_INSENSITIVE_ORDER));

        List<LiveProfitSharingOverviewDTO.PartnerLiveProfitDTO> partnerDTOs = new ArrayList<>();
        BigDecimal totalEarnedAll = BigDecimal.ZERO;
        BigDecimal totalWithdrawnAll = BigDecimal.ZERO;
        BigDecimal totalRemainingAll = BigDecimal.ZERO;

        for (Partner partner : partners) {
            BigDecimal earned = calculateLiveEarnedProfit(partner, netProfit);
            BigDecimal withdrawn = withdrawalRepo.sumWithdrawnByPartnerAndUser(partner, currentUser);
            if (withdrawn == null) withdrawn = BigDecimal.ZERO;

            BigDecimal remaining = earned.subtract(withdrawn);

            totalEarnedAll = totalEarnedAll.add(earned);
            totalWithdrawnAll = totalWithdrawnAll.add(withdrawn);
            totalRemainingAll = totalRemainingAll.add(remaining);

            LiveProfitSharingOverviewDTO.PartnerLiveProfitDTO pDto = new LiveProfitSharingOverviewDTO.PartnerLiveProfitDTO();
            pDto.setPartnerPublicId(partner.getPublicId());
            pDto.setPartnerName(partner.getPartnerName());
            pDto.setPartnerEmail(partner.getEmail());
            pDto.setPartnerPhone(partner.getMobileNumber());
            pDto.setSharePercentage(partner.getSharePercentage());
            pDto.setActive(Boolean.TRUE.equals(partner.getIsActive()));
            pDto.setTotalEarnedProfit(earned);
            pDto.setTotalWithdrawnProfit(withdrawn);
            pDto.setRemainingProfitAvailable(remaining);

            partnerDTOs.add(pDto);
        }

        // Exact penny remainder handling for active partners if active shares sum to 100%
        BigDecimal remainder = netProfit.subtract(totalEarnedAll);
        if (remainder.compareTo(BigDecimal.ZERO) != 0 && !partnerDTOs.isEmpty()) {
            for (int i = partnerDTOs.size() - 1; i >= 0; i--) {
                LiveProfitSharingOverviewDTO.PartnerLiveProfitDTO lastP = partnerDTOs.get(i);
                if (lastP.isActive()) {
                    BigDecimal adjEarned = lastP.getTotalEarnedProfit().add(remainder);
                    lastP.setTotalEarnedProfit(adjEarned);
                    BigDecimal adjRemaining = adjEarned.subtract(lastP.getTotalWithdrawnProfit());
                    lastP.setRemainingProfitAvailable(adjRemaining);
                    totalEarnedAll = totalEarnedAll.add(remainder);
                    totalRemainingAll = totalRemainingAll.add(remainder);
                    break;
                }
            }
        }

        LiveProfitSharingOverviewDTO overview = new LiveProfitSharingOverviewDTO();
        overview.setTotalMoneyReceived(totalReceived);
        overview.setTotalMoneyPaid(totalPaid);
        overview.setTotalSalesRevenue(totalReceived);
        overview.setTotalPurchasesCost(totalPaid);
        overview.setTotalExpenses(totalExpenses);
        // Net profit = actual cash received − actual cash paid − expenses (no withdrawals deducted)
        overview.setNetProfit(netProfit);
        overview.setTotalDistributedProfit(totalEarnedAll);
        overview.setTotalProfitWithdrawn(totalWithdrawnAll);
        overview.setTotalRemainingProfit(totalRemainingAll);
        overview.setPartners(partnerDTOs);
        overview.setLatestDistribution(getLatestDistribution());

        return overview;
    }

    @Override
    @Transactional
    public ResponseProfitWithdrawalDTO recordWithdrawal(RequestProfitWithdrawalDTO dto) {
        log.info("SERVICE - request came in recordWithdrawal for partner {}", dto.getPartnerPublicId());
        User currentUser = currentUserService.getCurrentUser();

        Partner partner = partnerRepo.findByUserAndPublicId(currentUser, dto.getPartnerPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        // Compute current live profit context based on actual money received and paid
        BigDecimal totalReceived = salePaymentRepo.sumTotalReceivedByUser(currentUser);
        if (totalReceived == null) totalReceived = BigDecimal.ZERO;

        BigDecimal totalPaid = purchasePaymentRepo.sumTotalPaidByUser(currentUser);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        BigDecimal totalExpenses = expenseRepo.sumTotalExpensesByUserAndDateRange(currentUser, null, null);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal liveNetProfit = totalReceived.subtract(totalPaid).subtract(totalExpenses);

        BigDecimal totalEarned = calculateLiveEarnedProfit(partner, liveNetProfit);
        BigDecimal totalWithdrawn = withdrawalRepo.sumWithdrawnByPartnerAndUser(partner, currentUser);
        if (totalWithdrawn == null) totalWithdrawn = BigDecimal.ZERO;

        BigDecimal available = totalEarned.subtract(totalWithdrawn);

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Withdrawal amount must be greater than 0");
        }

        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Withdrawal not allowed: no profit is currently available for this partner (available: " + available + ")");
        }

        if (dto.getAmount().compareTo(available) > 0) {
            throw new InvalidRequestException("Withdrawal amount (" + dto.getAmount() + ") cannot exceed available profit (" + available + ")");
        }

        BigDecimal remainingAfter = available.subtract(dto.getAmount());

        PartnerProfitWithdrawal withdrawal = new PartnerProfitWithdrawal();
        withdrawal.setUser(currentUser);
        withdrawal.setPartner(partner);
        withdrawal.setWithdrawalDate(dto.getWithdrawalDate());
        withdrawal.setAmount(dto.getAmount());
        withdrawal.setAvailableBeforeWithdrawal(available);
        withdrawal.setRemainingAfterWithdrawal(remainingAfter);
        withdrawal.setPaymentMethod(dto.getPaymentMethod() != null ? dto.getPaymentMethod().trim() : null);
        withdrawal.setReferenceNumber(dto.getReferenceNumber() != null ? dto.getReferenceNumber().trim() : null);
        withdrawal.setNotes(dto.getNotes() != null ? dto.getNotes().trim() : null);

        withdrawal = withdrawalRepo.save(withdrawal);

        return mapToWithdrawalResponse(withdrawal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseProfitWithdrawalDTO> getWithdrawalHistory(LocalDate fromDate, LocalDate toDate) {
        log.info("SERVICE - request came in getWithdrawalHistory...");
        User currentUser = currentUserService.getCurrentUser();

        List<PartnerProfitWithdrawal> list;
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                throw new InvalidRequestException("fromDate must be before or equal to toDate");
            }
            list = withdrawalRepo.findByUserAndWithdrawalDateBetweenOrderByWithdrawalDateDescCreatedAtDesc(currentUser, fromDate, toDate);
        } else {
            list = withdrawalRepo.findByUserOrderByWithdrawalDateDescCreatedAtDesc(currentUser);
        }

        return list.stream().map(this::mapToWithdrawalResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteWithdrawal(UUID publicId) {
        log.info("SERVICE - request came in deleteWithdrawal for publicId: {}", publicId);
        User currentUser = currentUserService.getCurrentUser();

        PartnerProfitWithdrawal withdrawal;
        if (currentUser.getRole() == FinanceManangementSystem.demo.Enums.UserRole.ADMIN) {
            withdrawal = withdrawalRepo.findAll().stream()
                    .filter(w -> publicId.equals(w.getPublicId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Profit withdrawal record not found"));
        } else {
            withdrawal = withdrawalRepo.findByUserAndPublicId(currentUser, publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Profit withdrawal record not found"));
        }

        withdrawalRepo.delete(withdrawal);
        log.info("SERVICE - successfully deleted profit withdrawal: {}", publicId);
    }

    private BigDecimal calculateLiveEarnedProfit(Partner partner, BigDecimal netProfit) {
        if (Boolean.TRUE.equals(partner.getIsActive()) && partner.getSharePercentage() != null) {
            return netProfit.multiply(partner.getSharePercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private ResponseProfitWithdrawalDTO mapToWithdrawalResponse(PartnerProfitWithdrawal w) {
        ResponseProfitWithdrawalDTO resp = new ResponseProfitWithdrawalDTO();
        resp.setPublicId(w.getPublicId());
        resp.setPartnerPublicId(w.getPartner().getPublicId());
        resp.setPartnerName(w.getPartner().getPartnerName());
        resp.setPartnerSharePercentage(w.getPartner().getSharePercentage());
        resp.setWithdrawalDate(w.getWithdrawalDate());
        resp.setAmount(w.getAmount());
        resp.setAvailableBeforeWithdrawal(w.getAvailableBeforeWithdrawal());
        resp.setRemainingAfterWithdrawal(w.getRemainingAfterWithdrawal());
        resp.setPaymentMethod(w.getPaymentMethod());
        resp.setReferenceNumber(w.getReferenceNumber());
        resp.setNotes(w.getNotes());
        resp.setCreatedAt(w.getCreatedAt());
        return resp;
    }
}
