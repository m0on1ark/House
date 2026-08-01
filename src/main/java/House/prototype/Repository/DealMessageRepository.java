package House.prototype.Repository;

import House.prototype.Model.DealMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealMessageRepository extends JpaRepository<DealMessage, Long> {
    @EntityGraph(attributePaths = {"author"})
    List<DealMessage> findByDealIdOrderByCreatedAtAsc(long dealId);

    void deleteByDealId(long dealId);
}
