package FinanceManangementSystem.demo.Service;

import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestProfitDistributionDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseProfitDistributionDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProfitDistributionServiceInterface {

    ResponseProfitDistributionDTO previewDistribution(
            RequestProfitDistributionDTO dto
    );

    ResponseProfitDistributionDTO calculateAndDistribute(
            RequestProfitDistributionDTO dto
    );

    ResponseProfitDistributionDTO getDistributionByPublicId(
            UUID publicId
    );

    List<ResponseProfitDistributionDTO> getAllDistributions();

    List<ResponseProfitDistributionDTO.PartnerShareDetails> getShareHistoryByPartner(
            UUID partnerPublicId
    );

    ResponseProfitDistributionDTO getLatestDistribution();

    BigDecimal getLifetimeEarningsByPartner(UUID partnerPublicId);

    FinanceManangementSystem.demo.Payloads.ResponseDTO.LiveProfitSharingOverviewDTO getLiveProfitOverview();

    FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseProfitWithdrawalDTO recordWithdrawal(
            FinanceManangementSystem.demo.Payloads.RequestDTO.RequestProfitWithdrawalDTO dto
    );

    List<FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseProfitWithdrawalDTO> getWithdrawalHistory(
            LocalDate fromDate,
            LocalDate toDate
    );

    void deleteWithdrawal(UUID publicId);
}
