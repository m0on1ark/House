package House.prototype.Service;

import House.prototype.Exception.InvalidRequestException;
import House.prototype.Model.Building;
import House.prototype.Model.User;
import House.prototype.Repository.BuildingRepository;
import House.prototype.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BuildingRepository buildingRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, buildingRepository, passwordEncoder);
    }

    private static User user(long id, String username, Building building) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setBuilding(building);
        return user;
    }

    private static Building building(long id) {
        Building building = new Building();
        building.setId(id);
        return building;
    }

    @Test
    void registerHashesThePasswordBeforeSaving() {
        given(userRepository.existsByEmail("anna@example.com")).willReturn(false);
        given(userRepository.existsByUsername("anna")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(call -> call.getArgument(0));

        User saved = userService.register("anna", "anna@example.com", "correct-horse");

        assertThat(saved.getPassword()).isNotEqualTo("correct-horse");
        assertThat(passwordEncoder.matches("correct-horse", saved.getPassword())).isTrue();
    }

    @Test
    void registerRefusesAUsernameThatIsAlreadyTaken() {
        given(userRepository.existsByEmail("anna@example.com")).willReturn(false);
        given(userRepository.existsByUsername("anna")).willReturn(true);

        assertThatThrownBy(() -> userService.register("anna", "anna@example.com", "correct-horse"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void changePasswordRefusesAWrongCurrentPassword() {
        User anna = user(7, "anna", null);
        anna.setPassword(passwordEncoder.encode("correct-horse"));
        given(userRepository.findById(7L)).willReturn(Optional.of(anna));

        assertThatThrownBy(() -> userService.changePassword(7, "wrong-guess", "new-secret"))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(passwordEncoder.matches("correct-horse", anna.getPassword())).isTrue();
    }

    @Test
    void aProfileIsHiddenFromResidentsOfAnotherBuilding() {
        User anna = user(7, "anna", building(1));
        User stranger = user(8, "bob", building(2));
        given(userRepository.findById(7L)).willReturn(Optional.of(anna));
        given(userRepository.findById(8L)).willReturn(Optional.of(stranger));

        assertThatThrownBy(() -> userService.neighbourProfile(7, 8))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void aProfileIsVisibleToSomeoneInTheSameBuilding() {
        Building shared = building(1);
        User anna = user(7, "anna", shared);
        User neighbour = user(8, "bob", shared);
        given(userRepository.findById(7L)).willReturn(Optional.of(anna));
        given(userRepository.findById(8L)).willReturn(Optional.of(neighbour));

        assertThat(userService.neighbourProfile(7, 8)).isSameAs(neighbour);
    }
}
