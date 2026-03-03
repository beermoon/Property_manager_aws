package kr.co.choi.property_manager.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "memos")
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, length = 2000)
    private String content;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }


    protected Memo() {}

    public Memo(String content) {
        this.content = content;
    }

    public Long getId() {return id;}
    public String getContent() {return content;}
    public LocalDateTime getCreatedAt() {return createdAt;}
    public Property getProperty() {return property;}

    void setProperty(Property property) {
        this.property = property;
    }

}
