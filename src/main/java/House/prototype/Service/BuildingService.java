package House.prototype.Service;

import House.prototype.DTO.BuildingSuggestion;
import House.prototype.External.NominatimResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class BuildingService {
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://nominatim.openstreetmap.org")
            .defaultHeader("User-Agent", "yo (ruslan.dzikanchuk@gmail.com)")
            .build();

    public List<BuildingSuggestion> findByPrefix(String query){
        if(query == null || query.isBlank()){
            return List.of();
        }
        NominatimResponse[] found = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("countrycodes", "pl")
                        .queryParam("format", "json")
                        .queryParam("addressdetails", 1)
                        .queryParam("limit", 10)
                        .build())
                .retrieve()
                .body(NominatimResponse[].class);
        if (found == null) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<BuildingSuggestion> mapped = Arrays.stream(found)
                .filter(n -> n.getAddress() != null && n.getAddress().locality() != null)
                .map(n -> new BuildingSuggestion(
                        n.getOsmId(),
                        n.getDisplayName(),
                        n.getAddress().locality(),
                        n.getAddress().getRoad(),
                        n.getAddress().getHouseNumber(),
                        n.getAddress().getPostcode(),
                        Double.parseDouble(n.getLat()),
                        Double.parseDouble(n.getLon())
                        ))
                .filter(s -> seen.add(addressKey(s)))
                .toList();
        return Stream.concat(
                mapped.stream().filter(s -> s.number() != null),
                mapped.stream().filter(s -> s.number() == null).limit(3)
        ).toList();
    }

    private static String addressKey(BuildingSuggestion s) {
        return String.join("|",
                norm(s.city()), norm(s.street()), norm(s.number()), norm(s.postalCode()));
    }

    private static String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
