package House.prototype.Repository;

import House.prototype.Model.BuildingMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingMessageRepository extends JpaRepository<BuildingMessage, Long> {
    List<BuildingMessage> findByBuildingIdOrderByCreatedAtAsc(long buildingId);
}
