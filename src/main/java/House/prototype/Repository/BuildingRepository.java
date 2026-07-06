package House.prototype.Repository;

import House.prototype.Model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    Optional<Building> findByOsmId(long osmId);
}
