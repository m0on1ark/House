package House.prototype.Service;

import House.prototype.Exception.InvalidRequestException;
import House.prototype.Model.Building;
import House.prototype.Model.Deal;
import House.prototype.Model.DealStatus;
import House.prototype.Model.User;
import House.prototype.Repository.BuildingRepository;
import House.prototype.Repository.DealClaimRepository;
import House.prototype.Repository.DealRepository;
import House.prototype.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Not @Transactional on purpose: every thread must run in its own transaction, as real requests do.
@SpringBootTest
class DealClaimConcurrencyTest {

    private static final int THREADS = 10;

    @Autowired
    private DealService dealService;
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DealRepository dealRepository;
    @Autowired
    private DealClaimRepository dealClaimRepository;

    private Building building;
    private final List<User> users = new ArrayList<>();
    private Deal deal;

    @BeforeEach
    void setUp() {
        Building b = new Building();
        b.setOsmId(ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE));
        b.setCity("Krakow");
        b.setNumber("1");
        building = buildingRepository.save(b);

        User author = user("author");
        Deal d = new Deal();
        d.setBuilding(building);
        d.setAuthor(author);
        d.setProductName("Mineral water");
        d.setStoreName("Zabka");
        d.setTotalUnits(6);
        d.setAuthorUnits(1);
        deal = dealRepository.save(d);
    }

    @AfterEach
    void tearDown() {
        dealClaimRepository.findAll().stream()
                .filter(c -> c.getDeal().getId() == deal.getId())
                .forEach(dealClaimRepository::delete);
        dealRepository.deleteById(deal.getId());
        userRepository.deleteAll(users);
        buildingRepository.delete(building);
    }

    private User user(String name) {
        User u = new User();
        u.setUsername(name + ThreadLocalRandom.current().nextInt(1_000_000_000));
        u.setEmail(u.getUsername() + "@example.com");
        u.setPassword("hashed");
        u.setBuilding(building);
        User saved = userRepository.save(u);
        users.add(saved);
        return saved;
    }

    /** Runs every task at the same moment and returns how many of them succeeded. */
    private int runTogether(List<Callable<?>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Callable<?> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            int succeeded = 0;
            for (Future<?> f : futures) {
                try {
                    f.get(30, TimeUnit.SECONDS);
                    succeeded++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(InvalidRequestException.class);
                }
            }
            return succeeded;
        } finally {
            pool.shutdownNow();
        }
    }

    private int claimedUnits() {
        return dealClaimRepository.findAll().stream()
                .filter(c -> c.getDeal().getId() == deal.getId())
                .mapToInt(c -> c.getUnits())
                .sum();
    }

    @Test
    void parallelClaimsNeverTakeMoreUnitsThanTheDealHas() throws Exception {
        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            long claimerId = user("claimer").getId();
            tasks.add(() -> dealService.claim(deal.getId(), claimerId, 1));
        }

        int succeeded = runTogether(tasks);

        assertThat(succeeded).isEqualTo(5);
        assertThat(claimedUnits()).isEqualTo(5);
        assertThat(dealRepository.findById(deal.getId()).orElseThrow().getStatus())
                .isEqualTo(DealStatus.CLOSED);
    }

    @Test
    void aDoubleClickClaimsOnlyOnce() throws Exception {
        long claimerId = user("claimer").getId();
        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> dealService.claim(deal.getId(), claimerId, 1));
        }

        int succeeded = runTogether(tasks);

        assertThat(succeeded).isEqualTo(1);
        assertThat(claimedUnits()).isEqualTo(1);
    }
}
