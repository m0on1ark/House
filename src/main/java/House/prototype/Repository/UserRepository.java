package House.prototype.Repository;
import House.prototype.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);

    List<User> findByBuildingIdOrderByUsernameAsc(long buildingId);

}
