package kr.co.choi.property_manager.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "property_photos")
public class PropertyPhoto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String storedName;

    @Column(nullable = false)
    private String url;

    private LocalDateTime createdAt;

    protected PropertyPhoto() {}

    public PropertyPhoto(Property property, String originalName, String storedName, String url) {
        this.property = property;
        this.originalName = originalName;
        this.storedName = storedName;
        this.url = url;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public String getOriginalName() { return originalName; }
    public Property getProperty() { return property; }
}
