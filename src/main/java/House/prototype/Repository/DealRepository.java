package House.prototype.Repository;

import House.prototype.Model.Deal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DealRepository extends JpaRepository<Deal, Long> {
    @EntityGraph(attributePaths = {"author", "building", "claims", "claims.user"})
    List<Deal> findByBuildingIdInOrderByCreatedAtDesc(Collection<Long> buildingIds);

    @EntityGraph(attributePaths = {"claims"})
    List<Deal> findByAuthorIdOrderByCreatedAtDesc(long authorId);
}
