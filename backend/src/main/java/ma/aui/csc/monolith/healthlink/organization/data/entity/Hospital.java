package ma.aui.csc.monolith.healthlink.organization.data.entity;

import jakarta.persistence.*;
import ma.aui.csc.monolith.healthlink.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hospitals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hospital extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 250)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    // WGS 84 coordinates (the GPS system). Null until an administrator records the location;
    // hospitals without coordinates are simply left out of nearby searches.
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    public Hospital(
            String code,
            String name,
            String city,
            String address,
            Region region
    ) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.address = address;
        this.region = region;
    }

    public Hospital(String code, String name, String city, String address, Region region,
                    Double latitude, Double longitude) {
        this(code, name, city, address, region);
        relocate(latitude, longitude);
    }

    /** Records where the hospital is. Both values are set together, or both are cleared. */
    public void relocate(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Latitude and longitude must be provided together");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
