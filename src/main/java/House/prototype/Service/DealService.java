package House.prototype.Service;

import House.prototype.Model.Building;
import House.prototype.Model.Deal;
import House.prototype.Model.DealClaim;
import House.prototype.Model.DealStatus;
import House.prototype.Model.User;
import House.prototype.Repository.DealClaimRepository;
import House.prototype.Repository.DealMessageRepository;
import House.prototype.Repository.DealRepository;
import House.prototype.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DealService {
    private final DealClaimRepository dealClaimRepository;
    private final UserRepository userRepository;
    private final DealRepository dealRepository;
    private final DealMessageRepository dealMessageRepository;

    public DealService(UserRepository userRepository, DealRepository dealRepository,
                       DealClaimRepository dealClaimRepository,
                       DealMessageRepository dealMessageRepository){
        this.userRepository = userRepository;
        this.dealRepository = dealRepository;
        this.dealClaimRepository = dealClaimRepository;
        this.dealMessageRepository = dealMessageRepository;
    }
    public Deal createDeal(long authorId,
                             String productName,
                             String storeName,
                             int totalUnits,
                             int authorUnits,
                             Double pricePerUnit,
                             byte[] image,
                             String imageType){

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid"));
        Building authorBuilding = author.getBuilding();
        if (authorBuilding == null) {
            throw new IllegalArgumentException("link a building first");
        }
        if (productName == null || productName.isBlank()
                || storeName == null || storeName.isBlank()) {
            throw new IllegalArgumentException("product and store are required");
        }
        if (totalUnits < 2 || authorUnits < 1 || authorUnits >= totalUnits) {
            throw new IllegalArgumentException("bad unit counts");
        }
        Deal deal = new Deal();
        deal.setAuthor(author);
        deal.setAuthorUnits(authorUnits);
        deal.setStoreName(storeName);
        deal.setTotalUnits(totalUnits);
        deal.setProductName(productName);
        deal.setPricePerUnit(pricePerUnit);
        deal.setBuilding(authorBuilding);
        if (image != null && image.length > 0) {
            deal.setImage(image);
            deal.setImageType(imageType);
        }
        return dealRepository.save(deal);
    }

    public Deal getById(long dealId) {
        return dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid"));
    }

    @Transactional
    public void deleteDeal(long dealId, long userId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid"));
        if (deal.getAuthor().getId() != userId) {
            throw new IllegalArgumentException("not your deal");
        }
        dealClaimRepository.deleteByDealId(dealId);
        dealMessageRepository.deleteByDealId(dealId);
        dealRepository.delete(deal);
    }

    public List<Deal> findByAuthor(long userId) {
        return dealRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
    }
    public DealClaim claim(long dealId, long userId, int units){
        User claimer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid"));
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid"));
        if (claimer.getBuilding() == null
                || claimer.getBuilding().getId() != deal.getBuilding().getId()){
            throw new IllegalArgumentException("Not your house");
        }
        if (claimer.getId() == deal.getAuthor().getId()){
            throw new IllegalArgumentException("author has his share");
        }
        if (units < 1) {
            throw new IllegalArgumentException("bad count");
        }
        if (dealClaimRepository.findByDealIdAndUserId(dealId, userId).isPresent()) {
            throw new IllegalArgumentException("already claimed");
        }
        if (units > deal.remainingUnits()) {
            throw new IllegalArgumentException("not enough units left");
        }
        boolean last = units == deal.remainingUnits();
        DealClaim dealClaim = new DealClaim();
        dealClaim.setUnits(units);
        dealClaim.setDeal(deal);
        dealClaim.setUser(claimer);
        dealClaim = dealClaimRepository.save(dealClaim);
        if (last) {
            deal.setStatus(DealStatus.CLOSED);
            dealRepository.save(deal);
        }
        return dealClaim;
    }

    public void cancelClaim(long dealId, long userId){
        DealClaim claim = dealClaimRepository.findByDealIdAndUserId(dealId, userId)
                .orElseThrow(() -> new IllegalArgumentException("nothing to cancel"));
        Deal deal = claim.getDeal();
        dealClaimRepository.delete(claim);
        if (deal.getStatus() == DealStatus.CLOSED) {
            deal.setStatus(DealStatus.OPEN);
            dealRepository.save(deal);
        }
    }

    public List<Deal> findForUser(long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid"));
        if (user.getBuilding() == null){
            return List.of();
        }
        return dealRepository.findByBuildingIdOrderByCreatedAtDesc(user.getBuilding().getId());
    }

}
