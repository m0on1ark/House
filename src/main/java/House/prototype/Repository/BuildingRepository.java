package House.prototype.Repository;

import House.prototype.Model.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    Optional<Building> findByOsmId(long osmId);

    @Query(value = """
            select b.id from buildings b
            where b.lat is not null and b.lon is not null
              and 2 * 6371 * asin(sqrt(
                    power(sin(radians(b.lat - :lat) / 2), 2)
                  + cos(radians(:lat)) * cos(radians(b.lat))
                    * power(sin(radians(b.lon - :lon) / 2), 2)
              )) <= :radiusKm
            """, nativeQuery = true)
    List<Long> findIdsWithinKm(@Param("lat") double lat,
                               @Param("lon") double lon,
                               @Param("radiusKm") double radiusKm);
}
