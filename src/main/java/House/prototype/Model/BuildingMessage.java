package House.prototype.Model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "building_messages")
public class BuildingMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id")
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "text", nullable = false, length = 1000)
    private String text;

    @Column(name = "image")
    private byte[] image;

    @Column(name = "image_type", length = 100)
    private String imageType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public boolean hasImage() {
        return image != null && image.length > 0;
    }

    private static final DateTimeFormatter TIME_LABEL =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault());

    public String timeLabel() {
        return TIME_LABEL.format(createdAt);
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

    public String getText() {
        return text;
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

    public Instant getCreatedAt() {
        return createdAt;
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

    public void setText(String text) {
        this.text = text;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
