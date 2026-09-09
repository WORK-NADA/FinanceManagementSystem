package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;
import FinanceManangementSystem.demo.Model.Investment;
import FinanceManangementSystem.demo.Model.Partner;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestInvestmentDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseInvestmentDTO;
import FinanceManangementSystem.demo.Repository.InvestmentRepository;
import FinanceManangementSystem.demo.Repository.PartnerRepository;
import FinanceManangementSystem.demo.Service.InvestmentServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentService implements InvestmentServiceInterface {

    private final InvestmentRepository investmentRepo;
    private final PartnerRepository partnerRepo;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ResponseInvestmentDTO addInvestment(RequestInvestmentDTO dto) {
        log.info("SERVICE - adding investment for partner: {}", dto.getPartnerPublicId());

        User currentUser = currentUserService.getCurrentUser();

        Partner partner = partnerRepo.findByUserAndPublicId(currentUser, dto.getPartnerPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found or does not belong to the current company"));

        if (Boolean.FALSE.equals(partner.getIsActive())) {
            throw new InvalidRequestException("Cannot record investment for an inactive partner");
        }

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Investment amount must be greater than zero");
        }

        if (dto.getInvestmentDate() == null) {
            throw new InvalidRequestException("Investment date is required");
        }

        Investment investment = new Investment();
        investment.setUser(currentUser);
        investment.setPartner(partner);
        investment.setAmount(dto.getAmount());
        investment.setInvestmentDate(dto.getInvestmentDate());
        investment.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        Investment saved = investmentRepo.save(investment);
        log.info("SERVICE - investment added successfully with publicId: {}", saved.getPublicId());

        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public ResponseInvestmentDTO updateInvestment(UUID publicId, RequestInvestmentDTO dto) {
        log.info("SERVICE - updating investment: {}", publicId);

        User currentUser = currentUserService.getCurrentUser();

        Investment investment = investmentRepo.findByUserAndPublicId(currentUser, publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment record not found"));

        Partner partner = partnerRepo.findByUserAndPublicId(currentUser, dto.getPartnerPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found or does not belong to the current company"));

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Investment amount must be greater than zero");
        }

        if (dto.getInvestmentDate() == null) {
            throw new InvalidRequestException("Investment date is required");
        }

        investment.setPartner(partner);
        investment.setAmount(dto.getAmount());
        investment.setInvestmentDate(dto.getInvestmentDate());
        investment.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        Investment updated = investmentRepo.save(investment);
        log.info("SERVICE - investment updated successfully: {}", publicId);

        return mapToResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteInvestment(UUID publicId) {
        log.info("SERVICE - deleting investment: {}", publicId);

        User currentUser = currentUserService.getCurrentUser();

        Investment investment = investmentRepo.findByUserAndPublicId(currentUser, publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment record not found"));

        investmentRepo.delete(investment);
        log.info("SERVICE - investment deleted successfully: {}", publicId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseInvestmentDTO getInvestmentByPublicId(UUID publicId) {
        User currentUser = currentUserService.getCurrentUser();

        Investment investment = investmentRepo.findByUserAndPublicId(currentUser, publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment record not found"));

        return mapToResponseDTO(investment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseInvestmentDTO> getAllInvestments() {
        User currentUser = currentUserService.getCurrentUser();
        List<Investment> list = investmentRepo.findByUserOrderByInvestmentDateDescCreatedAtDesc(currentUser);

        return list.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalInvestments() {
        User currentUser = currentUserService.getCurrentUser();
        BigDecimal total = investmentRepo.sumTotalInvestmentsByUser(currentUser);
        return total != null ? total : BigDecimal.ZERO;
    }

    private ResponseInvestmentDTO mapToResponseDTO(Investment inv) {
        Partner partner = inv.getPartner();
        return ResponseInvestmentDTO.builder()
                .publicId(inv.getPublicId())
                .partnerPublicId(partner != null ? partner.getPublicId() : null)
                .partnerName(partner != null ? partner.getPartnerName() : "Unknown Partner")
                .partnerMobile(partner != null ? partner.getMobileNumber() : null)
                .partnerSharePercentage(partner != null ? partner.getSharePercentage() : BigDecimal.ZERO)
                .investmentDate(inv.getInvestmentDate())
                .amount(inv.getAmount())
                .description(inv.getDescription())
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }
}
