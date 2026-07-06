package House.prototype.External;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NominatimResponse {

    @JsonProperty("place_id")
    private long placeId;

    @JsonProperty("osm_id")
    private long osmId;

    @JsonProperty("display_name")
    private String displayName;

    private String lat;
    private String lon;

    private Address address;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        private String road;

        @JsonProperty("house_number")
        private String houseNumber;

        private String city;
        private String town;
        private String village;

        private String postcode;

        public String locality() {
            if (city != null) return city;
            if (town != null) return town;
            return village;
        }

        public String getRoad() { return road; }
        public void setRoad(String road) { this.road = road; }
        public String getHouseNumber() { return houseNumber; }
        public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getTown() { return town; }
        public void setTown(String town) { this.town = town; }
        public String getVillage() { return village; }
        public void setVillage(String village) { this.village = village; }
        public String getPostcode() { return postcode; }
        public void setPostcode(String postcode) { this.postcode = postcode; }
    }

    public long getPlaceId() { return placeId; }
    public void setPlaceId(long placeId) { this.placeId = placeId; }
    public long getOsmId() { return osmId; }
    public void setOsmId(long osmId) { this.osmId = osmId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLat() { return lat; }
    public void setLat(String lat) { this.lat = lat; }
    public String getLon() { return lon; }
    public void setLon(String lon) { this.lon = lon; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}
