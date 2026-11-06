package House.prototype.Service;

import House.prototype.Exception.InvalidRequestException;
import House.prototype.Model.Building;
import House.prototype.Model.BuildingMessage;
import House.prototype.Model.Deal;
import House.prototype.Model.DealMessage;
import House.prototype.Model.User;
import House.prototype.Repository.BuildingMessageRepository;
import House.prototype.Repository.DealClaimRepository;
import House.prototype.Repository.DealMessageRepository;
import House.prototype.Repository.DealRepository;
import House.prototype.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {
    private final DealRepository dealRepository;
    private final DealClaimRepository dealClaimRepository;
    private final DealMessageRepository dealMessageRepository;
    private final BuildingMessageRepository buildingMessageRepository;
    private final UserRepository userRepository;

    public ChatService(DealRepository dealRepository,
                       DealClaimRepository dealClaimRepository,
                       DealMessageRepository dealMessageRepository,
                       BuildingMessageRepository buildingMessageRepository,
                       UserRepository userRepository) {
        this.dealRepository = dealRepository;
        this.dealClaimRepository = dealClaimRepository;
        this.dealMessageRepository = dealMessageRepository;
        this.buildingMessageRepository = buildingMessageRepository;
        this.userRepository = userRepository;
    }

    private User loadUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid"));
    }

    private String cleanBody(String text, boolean hasImage) {
        String body = text == null ? "" : text.trim();
        if (body.isEmpty() && !hasImage) {
            throw new InvalidRequestException("empty message");
        }
        if (body.length() > 1000) {
            throw new InvalidRequestException("too long");
        }
        return body;
    }

    private void checkParticipant(Deal deal, long userId) {
        boolean isAuthor = deal.getAuthor().getId() == userId;
        boolean hasClaim = dealClaimRepository
                .findByDealIdAndUserId(deal.getId(), userId).isPresent();
        if (!isAuthor && !hasClaim) {
            throw new InvalidRequestException("not a participant");
        }
    }

    public Deal dealFor(long dealId, long userId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new InvalidRequestException("Invalid"));
        checkParticipant(deal, userId);
        return deal;
    }

    public List<DealMessage> dealMessages(long dealId, long userId) {
        dealFor(dealId, userId);
        return dealMessageRepository.findByDealIdOrderByCreatedAtAsc(dealId);
    }

    @Transactional
    public DealMessage sendDealMessage(long dealId, long userId, String text,
                                       byte[] image, String imageType) {
        Deal deal = dealRepository.findByIdForShare(dealId)
                .orElseThrow(() -> new InvalidRequestException("Invalid"));
        checkParticipant(deal, userId);
        boolean hasImage = image != null && image.length > 0;
        DealMessage message = new DealMessage();
        message.setDeal(deal);
        message.setAuthor(loadUser(userId));
        message.setText(cleanBody(text, hasImage));
        if (hasImage) {
            message.setImage(image);
            message.setImageType(imageType);
        }
        return dealMessageRepository.save(message);
    }

    public DealMessage dealMessage(long messageId, long userId) {
        DealMessage message = dealMessageRepository.findById(messageId)
                .orElseThrow(() -> new InvalidRequestException("Invalid"));
        checkParticipant(message.getDeal(), userId);
        return message;
    }

    public Building buildingFor(long userId) {
        Building building = loadUser(userId).getBuilding();
        if (building == null) {
            throw new InvalidRequestException("link a building first");
        }
        return building;
    }

    public List<BuildingMessage> buildingMessages(long userId) {
        return buildingMessageRepository
                .findByBuildingIdOrderByCreatedAtAsc(buildingFor(userId).getId());
    }

    public BuildingMessage sendBuildingMessage(long userId, String text,
                                               byte[] image, String imageType) {
        Building building = buildingFor(userId);
        boolean hasImage = image != null && image.length > 0;
        BuildingMessage message = new BuildingMessage();
        message.setBuilding(building);
        message.setAuthor(loadUser(userId));
        message.setText(cleanBody(text, hasImage));
        if (hasImage) {
            message.setImage(image);
            message.setImageType(imageType);
        }
        return buildingMessageRepository.save(message);
    }

    public BuildingMessage buildingMessage(long messageId, long userId) {
        BuildingMessage message = buildingMessageRepository.findById(messageId)
                .orElseThrow(() -> new InvalidRequestException("Invalid"));
        Building building = loadUser(userId).getBuilding();
        if (building == null || building.getId() != message.getBuilding().getId()) {
            throw new InvalidRequestException("not your building");
        }
        return message;
    }
}
