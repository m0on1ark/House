package House.prototype.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "deal_claims",
        uniqueConstraints = @UniqueConstraint(columnNames = {"deal_id", "user_id"}))
public class DealClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "units", nullable = false)
    private int units;

    public long getId() {
        return id;
    }

    public Deal getDeal() {
        return deal;
    }

    public User getUser() {
        return user;
    }

    public int getUnits() {
        return units;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDeal(Deal deal) {
        this.deal = deal;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setUnits(int units) {
        this.units = units;
    }
}
