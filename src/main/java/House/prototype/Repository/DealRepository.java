package House.prototype.Repository;

import House.prototype.Model.Deal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealRepository extends JpaRepository<Deal, Long> {
    List<Deal> findByBuildingIdOrderByCreatedAtDesc(long buildingId);

    List<Deal> findByAuthorIdOrderByCreatedAtDesc(long authorId);

    List<Deal> findAllByOrderByCreatedAtDesc();
}
