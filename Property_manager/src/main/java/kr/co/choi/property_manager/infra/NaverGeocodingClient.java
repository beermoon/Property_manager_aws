package kr.co.choi.property_manager.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class NaverGeocodingClient {

    private final RestClient restClient;
    private final String keyId;
    private final String key;

    public NaverGeocodingClient(
            @Value("${naver.geocoding.key-id}") String keyId,
            @Value("${naver.geocoding.key}") String key
    ) {
        this.restClient = RestClient.create();
        this.keyId = keyId;
        this.key = key;



    }



    public GeoPoint geocodeOrThrow(String address) {
        URI uri = UriComponentsBuilder
                .fromUriString("https://maps.apigw.ntruss.com/map-geocode/v2/geocode")
                .queryParam("query", address)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        GeocodeResponse res = restClient.get()
                .uri(uri)
                .header("X-NCP-APIGW-API-KEY-ID", keyId)
                .header("X-NCP-APIGW-API-KEY", key)
                .retrieve()
                .body(GeocodeResponse.class);

        if (res == null || res.addresses == null || res.addresses.isEmpty()) {
            throw new IllegalArgumentException("정확한 도로명 주소, 지번 주소 또는 우편번호를 입력해주세요.");
        }

        if (res.addresses.size() > 1) {
            throw new IllegalArgumentException(
                    "주소가 모호합니다. 도로명 + 건물번호까지 입력해주세요."
            );
        }

        // 네이버 응답은 x= 경도 (lng) , y = 위도 (lat) 형태
        var first = res.addresses.get(0);
        double lng = Double.parseDouble(first.x);
        double lat = Double.parseDouble(first.y);

        return new GeoPoint(lat, lng);

    }

    public record GeoPoint(double lat, double lng) { }

    // 필요한 필드만 받는 DTO
    public static class GeocodeResponse {
        public List<AddressItem> addresses;

        public static class AddressItem {
            public String x; // lng
            public String y; // lat
        }
    }

}
