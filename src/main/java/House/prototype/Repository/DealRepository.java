package House.prototype.Repository;

import House.prototype.Model.Deal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long> {
    @EntityGraph(attributePaths = {"author", "building", "claims", "claims.user"})
    List<Deal> findByBuildingIdInOrderByCreatedAtDesc(Collection<Long> buildingIds);

    @EntityGraph(attributePaths = {"claims"})
    List<Deal> findByAuthorIdOrderByCreatedAtDesc(long authorId);

    // SELECT ... FOR UPDATE: claims, cancels and deletes of one deal run one at a time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deal d where d.id = :id")
    Optional<Deal> findByIdForUpdate(@Param("id") long id);

    // SELECT ... FOR SHARE: messages may be sent in parallel, but not while the deal is being deleted
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select d from Deal d where d.id = :id")
    Optional<Deal> findByIdForShare(@Param("id") long id);
}
