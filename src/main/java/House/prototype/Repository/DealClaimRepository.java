package House.prototype.Repository;

import House.prototype.Model.DealClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DealClaimRepository extends JpaRepository<DealClaim, Long> {

    Optional<DealClaim> findByDealIdAndUserId(long dealId, long userId);

    void deleteByDealId(long dealId);
}
