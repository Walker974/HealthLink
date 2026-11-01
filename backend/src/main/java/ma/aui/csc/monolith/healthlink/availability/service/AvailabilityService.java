package ma.aui.csc.monolith.healthlink.availability.service;

import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import ma.aui.csc.monolith.healthlink.availability.data.AvailabilityDtos.NearbyHospitalResponse;
import ma.aui.csc.monolith.healthlink.availability.data.repository.NearbyAvailabilityQuery;
import org.springframework.stereotype.Service;

/**
 * Public, account-free search: "which hospitals near me have a working MRI right now?"
 *
 * Privacy: the citizen's position is rounded to two decimals (about 1 km) before it is used,
 * cached or compared, and it is never stored in the database.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final NearbyAvailabilityQuery query;
    private final AvailabilityCache cache;

    public List<NearbyHospitalResponse> nearby(String imagingTypeCode, double latitude, double longitude,
                                               double radiusKm, int limit) {
        String type = imagingTypeCode.strip().toUpperCase(Locale.ROOT);
        double lat = roundToKilometre(latitude);
        double lng = roundToKilometre(longitude);
        String key = String.format(Locale.ROOT, "nearby:%s:%.2f:%.2f:%.0f:%d", type, lat, lng, radiusKm, limit);
        NearbyHospitalResponse[] result = cache.get(key, NearbyHospitalResponse[].class,
                () -> query.find(type, lat, lng, radiusKm * 1000, limit).toArray(NearbyHospitalResponse[]::new));
        return List.of(result);
    }

    static double roundToKilometre(double coordinate) {
        return Math.round(coordinate * 100.0) / 100.0;
    }
}
