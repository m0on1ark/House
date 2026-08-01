package House.prototype.Repository;

import House.prototype.Model.Building;
import House.prototype.Model.Deal;
import House.prototype.Model.DealClaim;
import House.prototype.Model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DealListingQueryTest {

    private static final double HOME_LAT = 50.0;
    private static final double HOME_LON = 20.0;

    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DealRepository dealRepository;
    @Autowired
    private DealClaimRepository dealClaimRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Building home;
    private Building nearby;
    private Building faraway;
    private Building noCoordinates;

    @BeforeEach
    void setUp() {
        home = building(HOME_LAT, HOME_LON);
        nearby = building(HOME_LAT + 0.01, HOME_LON);
        faraway = building(HOME_LAT + 0.5, HOME_LON);
        noCoordinates = building(null, null);
    }

    private Building building(Double lat, Double lon) {
        Building b = new Building();
        b.setOsmId(ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE));
        b.setCity("Krakow");
        b.setNumber("1");
        b.setLat(lat);
        b.setLon(lon);
        return buildingRepository.save(b);
    }

    private User user(String name, Building building) {
        User u = new User();
        u.setUsername(name + ThreadLocalRandom.current().nextInt(1_000_000));
        u.setEmail(u.getUsername() + "@example.com");
        u.setPassword("hashed");
        u.setBuilding(building);
        return userRepository.save(u);
    }

    private Deal deal(Building building, User author) {
        Deal d = new Deal();
        d.setBuilding(building);
        d.setAuthor(author);
        d.setProductName("Mineral water");
        d.setStoreName("Zabka");
        d.setTotalUnits(6);
        d.setAuthorUnits(2);
        return dealRepository.save(d);
    }

    @Test
    void aBuildingOneKilometreAwayIsInsideATwoKilometreRadius() {
        List<Long> ids = buildingRepository.findIdsWithinKm(HOME_LAT, HOME_LON, 2);

        assertThat(ids).contains(home.getId(), nearby.getId());
    }

    @Test
    void aBuildingFiftyKilometresAwayIsOutsideATwoKilometreRadius() {
        List<Long> ids = buildingRepository.findIdsWithinKm(HOME_LAT, HOME_LON, 2);

        assertThat(ids).doesNotContain(faraway.getId());
    }

    @Test
    void aWiderRadiusReachesTheFarawayBuilding() {
        List<Long> ids = buildingRepository.findIdsWithinKm(HOME_LAT, HOME_LON, 100);

        assertThat(ids).contains(faraway.getId());
    }

    @Test
    void aBuildingWithoutCoordinatesIsNeverMatched() {
        List<Long> ids = buildingRepository.findIdsWithinKm(HOME_LAT, HOME_LON, 20_000);

        assertThat(ids).doesNotContain(noCoordinates.getId());
    }

    @Test
    void theDealsListingLoadsItsAssociationsInASingleQuery() {
        User author = user("author", home);
        User claimer = user("claimer", home);
        Deal first = deal(home, author);
        Deal second = deal(home, author);
        for (Deal d : List.of(first, second)) {
            DealClaim claim = new DealClaim();
            claim.setDeal(d);
            claim.setUser(claimer);
            claim.setUnits(1);
            dealClaimRepository.save(claim);
        }
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Deal> deals = dealRepository.findByBuildingIdInOrderByCreatedAtDesc(Set.of(home.getId()));
        for (Deal d : deals) {
            d.getAuthor().getUsername();
            d.getBuilding().getCity();
            d.claimedUnits();
        }

        assertThat(deals).hasSize(2);
        assertThat(statistics.getPrepareStatementCount())
                .as("author, building and claims must come back with the deals, not one query each")
                .isEqualTo(1);
    }
}
