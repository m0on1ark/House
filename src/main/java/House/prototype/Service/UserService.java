package House.prototype.Service;
import House.prototype.Exception.InvalidRequestException;
import House.prototype.DTO.BuildingSuggestion;
import House.prototype.Model.Building;
import House.prototype.Model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import House.prototype.Repository.BuildingRepository;
import House.prototype.Repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, BuildingRepository buildingRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public User register(String username,String email,String password){
        if (userRepository.existsByEmail(email)){
            throw new InvalidRequestException("is taken");
        }
        if (userRepository.existsByUsername(username)){
            throw new InvalidRequestException("is taken");
        }
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        return saveUnique(user);
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
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new InvalidRequestException("no such resident"));
        Building viewerBuilding = viewer.getBuilding();
        Building targetBuilding = target.getBuilding();
        if (viewerBuilding == null || targetBuilding == null
                || viewerBuilding.getId() != targetBuilding.getId()){
            throw new InvalidRequestException("not a neighbour");
        }
        return target;
    }

    public User linkBuilding(long userId, BuildingSuggestion s){
        if (s.osmId() == null
                || s.city() == null || s.city().isBlank()
                || s.number() == null || s.number().isBlank()){
            throw new InvalidRequestException("address not selected");
        }
        User user = getById(userId);
        buildingRepository.insertIfAbsent(s.osmId(), s.city(), s.street(), s.number(),
                s.postalCode(), s.lat(), s.lon());
        Building building = buildingRepository.findByOsmId(s.osmId())
                .orElseThrow(() -> new InvalidRequestException("address not selected"));
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
            throw new InvalidRequestException("username required");
        }
        String name = newUsername.trim();
        User user = getById(userId);
        if (!name.equals(user.getUsername()) && userRepository.existsByUsername(name)){
            throw new InvalidRequestException("is taken");
        }
        user.setUsername(name);
        return saveUnique(user);
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
            throw new InvalidRequestException("email required");
        }
        String email = newEmail.trim();
        User user = getById(userId);
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)){
            throw new InvalidRequestException("is taken");
        }
        user.setEmail(email);
        return saveUnique(user);
    }

    public User changePassword(long userId, String currentPassword, String newPassword){
        if (newPassword == null || newPassword.isBlank()){
            throw new InvalidRequestException("password required");
        }
        User user = getById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())){
            throw new InvalidRequestException("wrong password");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        return userRepository.save(user);
    }

    // the exists* checks above can race with a parallel request; the unique constraints can't
    private User saveUnique(User user){
        try{
            return userRepository.save(user);
        }catch (DataIntegrityViolationException e){
            throw new InvalidRequestException("is taken");
        }
    }

    private static String trimToNull(String value){
        if (value == null){
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
