package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Exceptions.InvalidRequestException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;

import FinanceManangementSystem.demo.Model.Partner;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestPartnerDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponsePartnerDTO;
import FinanceManangementSystem.demo.Repository.PartnerProfitShareRepository;
import FinanceManangementSystem.demo.Repository.PartnerRepository;
import FinanceManangementSystem.demo.Service.PartnerServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService implements PartnerServiceInterface {

    private final PartnerRepository partnerRepo;

    private final PartnerProfitShareRepository partnerProfitShareRepo;

    private final CurrentUserService currentUserService;

    private final ModelMapper modelMapper;


    @Override
    @Transactional
    public ResponsePartnerDTO addPartner(RequestPartnerDTO dto) {

        log.info("SERVICE - request came in addPartner...");

        User currentUser = currentUserService.getCurrentUser();

        if (partnerRepo.existsByUserAndMobileNumber(currentUser, dto.getMobileNumber())) {
            log.info("SERVICE - partner mobile number already exists for user...");
            throw new InvalidRequestException("Partner with this mobile number already exists");
        }

        BigDecimal currentSum = partnerRepo.sumActiveSharePercentage(currentUser);

        if (currentSum == null) currentSum = BigDecimal.ZERO;

        if (currentSum.compareTo(new BigDecimal("100.00")) >= 0) {
            log.info("SERVICE - partner share is already 100%...");
            throw new InvalidRequestException("Partner share is already 100%. No additional share is available.");
        }

        BigDecimal remaining = new BigDecimal("100.00").subtract(currentSum);
        if (dto.getSharePercentage().compareTo(remaining) > 0) {
            log.info("SERVICE - total partner share would exceed 100% (available: {}%)...", remaining);
            throw new InvalidRequestException("Total partner share cannot exceed 100%. Available share is " + remaining + "%.");
        }

        Partner partner = modelMapper.map(dto, Partner.class);
        partner.setUser(currentUser);

        partner = partnerRepo.save(partner);

        return mapToResponse(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponsePartnerDTO getPartnerByPublicId(UUID publicId) {
        log.info("SERVICE - request came in getPartnerByPublicId...");

        User currentUser = currentUserService.getCurrentUser();

        Partner partner = partnerRepo.findByUserAndPublicId(currentUser, publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        return mapToResponse(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePartnerDTO> getAllPartners() {
        log.info("SERVICE - request came in getAllPartners...");

        User currentUser = currentUserService.getCurrentUser();

        List<Partner> partners = partnerRepo.findByUser(currentUser);

        return partners.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePartnerDTO> getAllActivePartners() {
        log.info("SERVICE - request came in getAllActivePartners...");

        User currentUser = currentUserService.getCurrentUser();

        List<Partner> partners = partnerRepo.findByUserAndIsActiveTrue(currentUser);

        return partners.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponsePartnerDTO updatePartner(UUID publicId, RequestPartnerDTO dto) {
        log.info("SERVICE - request came in updatePartner...");

        User currentUser = currentUserService.getCurrentUser();

        Partner partner = partnerRepo.findByUserAndPublicId(currentUser, publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        if (!partner.getMobileNumber().equals(dto.getMobileNumber())
                && partnerRepo.existsByUserAndMobileNumberAndPublicIdNot(currentUser, dto.getMobileNumber(), publicId)) {
            log.info("SERVICE - partner mobile number already exists for user...");
            throw new InvalidRequestException("Partner with this mobile number already exists");
        }

        BigDecimal otherSum;
        if (Boolean.TRUE.equals(partner.getIsActive())) {
            otherSum = partnerRepo.sumActiveSharePercentageExcluding(currentUser, publicId);
        } else {
            otherSum = partnerRepo.sumActiveSharePercentage(currentUser);
        }
        if (otherSum == null) otherSum = BigDecimal.ZERO;

        BigDecimal maxAllowed = new BigDecimal("100.00").subtract(otherSum);
        if (dto.getSharePercentage().compareTo(maxAllowed) > 0) {
            log.info("SERVICE - total partner share would exceed 100% on update (max allowed: {}%)...", maxAllowed);
            throw new InvalidRequestException("Total active partner share cannot exceed 100%. Maximum available share is " + maxAllowed + "%.");
        }

        partner.setPartnerName(dto.getPartnerName());
        partner.setMobileNumber(dto.getMobileNumber());
        partner.setEmail(dto.getEmail());
        partner.setSharePercentage(dto.getSharePercentage());
        partner.setJoiningDate(dto.getJoiningDate());

        partner = partnerRepo.save(partner);

        return mapToResponse(partner);
    }

    @Override
    @Transactional
    public void deactivatePartner(UUID publicId) {
        log.info("SERVICE - request came in deactivatePartner...");

        User currentUser = currentUserService.getCurrentUser();

        Partner partner = partnerRepo.findByUserAndPublicId(currentUser, publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        if (!partner.getIsActive()) {
            return;
        }

        partner.setIsActive(false);
        partnerRepo.save(partner);
    }

    @Override
    @Transactional
    public void reactivatePartner(UUID publicId) {
        log.info("SERVICE - request came in reactivatePartner...");

        User currentUser = currentUserService.getCurrentUser();

        Partner partner = partnerRepo.findByUserAndPublicId(currentUser, publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        if (Boolean.TRUE.equals(partner.getIsActive())) {
            return;
        }

        BigDecimal currentSum = partnerRepo.sumActiveSharePercentage(currentUser);
        if (currentSum == null) currentSum = BigDecimal.ZERO;

        if (currentSum.add(partner.getSharePercentage()).compareTo(new BigDecimal("100.00")) > 0) {
            BigDecimal available = new BigDecimal("100.00").subtract(currentSum);
            log.info("SERVICE - reactivating partner would exceed 100% active share (available: {}%)...", available);
            throw new InvalidRequestException("Cannot reactivate partner. Total active partner share would exceed 100% (Available share: " + available + "%).");
        }

        partner.setIsActive(true);
        partnerRepo.save(partner);
    }

    private ResponsePartnerDTO mapToResponse(Partner partner) {

        BigDecimal lifetime = partnerProfitShareRepo.sumLifetimeEarningsByPartner(partner.getPublicId());

        if (lifetime == null) lifetime = BigDecimal.ZERO;

        ResponsePartnerDTO resp = modelMapper.map(partner, ResponsePartnerDTO.class);
        resp.setLifetimeEarnings(lifetime);

        return resp;
    }
}
