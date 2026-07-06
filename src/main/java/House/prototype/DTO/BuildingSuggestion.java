package House.prototype.DTO;

public record BuildingSuggestion(
        Long osmId,
        String label,
        String city,
        String street,
        String number,
        String postalCode,
        Double lat,
        Double lon
) {}
