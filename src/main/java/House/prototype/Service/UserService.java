package House.prototype.Service;
import House.prototype.DTO.BuildingSuggestion;
import House.prototype.Model.Building;
import House.prototype.Model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import House.prototype.Repository.BuildingRepository;
import House.prototype.Repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    public UserService(UserRepository userRepository, BuildingRepository buildingRepository) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
    }
    public User register(String username,String email,String password){
        if (userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("is taken");
        }
        if (userRepository.existsByUsername(username)){
            throw new IllegalArgumentException("is taken");
        }
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public User getById(long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid"));
    }

    public java.util.List<User> neighbours(long userId){
        User user = getById(userId);
        Building building = user.getBuilding();
        if (building == null){
            return java.util.List.of();
        }
        return userRepository.findByBuildingIdOrderByUsernameAsc(building.getId()).stream()
                .filter(u -> u.getId() != userId)
                .toList();
    }

    public User neighbourProfile(long viewerId, long targetId){
        User viewer = getById(viewerId);
        User target = getById(targetId);
        Building viewerBuilding = viewer.getBuilding();
        Building targetBuilding = target.getBuilding();
        if (viewerBuilding == null || targetBuilding == null
                || viewerBuilding.getId() != targetBuilding.getId()){
            throw new IllegalArgumentException("not a neighbour");
        }
        return target;
    }

    public User linkBuilding(long userId, BuildingSuggestion s){
        if (s.osmId() == null
                || s.city() == null || s.city().isBlank()
                || s.number() == null || s.number().isBlank()){
            throw new IllegalArgumentException("address not selected");
        }
        User user = getById(userId);
        Building building = buildingRepository
                .findByOsmId(s.osmId())
                .orElseGet(() -> {
                    Building b = new Building();
                    b.setOsmId(s.osmId());
                    b.setCity(s.city());
                    b.setStreet(s.street());
                    b.setNumber(s.number());
                    b.setPostalCode(s.postalCode());
                    b.setLat(s.lat());
                    b.setLon(s.lon());
                    return buildingRepository.save(b);
                });
        user.setBuilding(building);
        return userRepository.save(user);
    }

    public User setAvatar(long userId, byte[] avatar, String avatarType){
        User user = getById(userId);
        user.setAvatar(avatar);
        user.setAvatarType(avatarType);
        return userRepository.save(user);
    }

    public User changeUsername(long userId, String newUsername){
        if (newUsername == null || newUsername.isBlank()){
            throw new IllegalArgumentException("username required");
        }
        String name = newUsername.trim();
        User user = getById(userId);
        if (!name.equals(user.getUsername()) && userRepository.existsByUsername(name)){
            throw new IllegalArgumentException("is taken");
        }
        user.setUsername(name);
        return userRepository.save(user);
    }

    public User updateProfile(long userId, String fullName, String phone, String bio,
                              String floor, String apartment){
        User user = getById(userId);
        user.setFullName(trimToNull(fullName));
        user.setPhone(trimToNull(phone));
        user.setBio(trimToNull(bio));
        user.setFloor(trimToNull(floor));
        user.setApartment(trimToNull(apartment));
        return userRepository.save(user);
    }

    public User changeEmail(long userId, String newEmail){
        if (newEmail == null || newEmail.isBlank()){
            throw new IllegalArgumentException("email required");
        }
        String email = newEmail.trim();
        User user = getById(userId);
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("is taken");
        }
        user.setEmail(email);
        return userRepository.save(user);
    }

    public User changePassword(long userId, String currentPassword, String newPassword){
        if (newPassword == null || newPassword.isBlank()){
            throw new IllegalArgumentException("password required");
        }
        User user = getById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())){
            throw new IllegalArgumentException("wrong password");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        return userRepository.save(user);
    }

    private static String trimToNull(String value){
        if (value == null){
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public User login(String username, String password){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid"));
        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new IllegalArgumentException("Invalid");
        }
        return user;
    }

}
