package House.prototype.Model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deals")
public class Deal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "building_id")
    private Building building;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "total_units", nullable = false)
    private int totalUnits;

    @Column(name = "author_units", nullable = false)
    private int authorUnits;

    @Column(name = "price_per_unit")
    private Double pricePerUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DealStatus status = DealStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "image")
    private byte[] image;

    @Column(name = "image_type", length = 100)
    private String imageType;

    @OneToMany(mappedBy = "deal")
    private List<DealClaim> claims = new ArrayList<>();

    public int claimedUnits() {
        int sum = authorUnits;
        for (DealClaim c : claims) {
            sum += c.getUnits();
        }
        return sum;
    }

    public int remainingUnits() {
        return totalUnits - claimedUnits();
    }

    public boolean hasImage() {
        return image != null && image.length > 0;
    }

    public long getId() {
        return id;
    }

    public Building getBuilding() {
        return building;
    }

    public User getAuthor() {
        return author;
    }

    public String getProductName() {
        return productName;
    }

    public String getStoreName() {
        return storeName;
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    public int getAuthorUnits() {
        return authorUnits;
    }

    public Double getPricePerUnit() {
        return pricePerUnit;
    }

    public DealStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<DealClaim> getClaims() {
        return claims;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void setTotalUnits(int totalUnits) {
        this.totalUnits = totalUnits;
    }

    public void setAuthorUnits(int authorUnits) {
        this.authorUnits = authorUnits;
    }

    public void setPricePerUnit(Double pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public void setStatus(DealStatus status) {
        this.status = status;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
