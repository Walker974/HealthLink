package ma.aui.csc.monolith.healthlink.availability.data.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import ma.aui.csc.monolith.healthlink.availability.data.AvailabilityDtos.NearbyHospitalResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Geographic search with PostGIS. ST_DWithin keeps hospitals within the radius and ST_Distance
 * sorts them, both measured on the Earth's surface (geography type, in metres).
 *
 * The availability rules match the city search: active hospital, department, imaging type and
 * machine, status AVAILABLE, and no unverified work order on the machine.
 *
 * Plain SQL (not JPQL) because JPQL does not know PostGIS functions.
 */
@Repository
public class NearbyAvailabilityQuery {

    public static final String SQL = """
            SELECT h.id AS hospital_id, h.name AS hospital_name, h.city, h.address,
                   h.latitude, h.longitude,
                   COUNT(e.id) AS available_count,
                   MAX(e.status_updated_at) AS last_status_change,
                   ST_Distance(
                       CAST(ST_SetSRID(ST_MakePoint(h.longitude, h.latitude), 4326) AS geography),
                       CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)) AS distance_m
            FROM hospitals h
            JOIN departments d ON d.hospital_id = h.id AND d.active = TRUE
            JOIN equipment e ON e.department_id = d.id AND e.active = TRUE AND e.status = 'AVAILABLE'
            JOIN imaging_types t ON t.id = e.imaging_type_id AND t.active = TRUE AND t.code = :type
            WHERE h.active = TRUE
              AND h.latitude IS NOT NULL AND h.longitude IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM work_orders w WHERE w.open_equipment_id = e.id)
              AND ST_DWithin(
                       CAST(ST_SetSRID(ST_MakePoint(h.longitude, h.latitude), 4326) AS geography),
                       CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
                       :radius)
            GROUP BY h.id, h.name, h.city, h.address, h.latitude, h.longitude
            ORDER BY distance_m, h.id
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public NearbyAvailabilityQuery(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public List<NearbyHospitalResponse> find(String imagingTypeCode, double latitude, double longitude,
                                             double radiusMetres, int limit) {
        return jdbc.query(SQL, Map.of(
                "type", imagingTypeCode,
                "lat", latitude,
                "lng", longitude,
                "radius", radiusMetres,
                "limit", limit), NearbyAvailabilityQuery::row);
    }

    private static NearbyHospitalResponse row(ResultSet rs, int rowNumber) throws SQLException {
        Timestamp lastChange = rs.getTimestamp("last_status_change");
        double distanceKm = Math.round(rs.getDouble("distance_m") / 100.0) / 10.0;
        return new NearbyHospitalResponse(
                rs.getLong("hospital_id"),
                rs.getString("hospital_name"),
                rs.getString("city"),
                rs.getString("address"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                distanceKm,
                rs.getLong("available_count"),
                lastChange == null ? null : lastChange.toInstant());
    }
}
