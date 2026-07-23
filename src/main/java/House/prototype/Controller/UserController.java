package House.prototype.Controller;

import House.prototype.DTO.BuildingSuggestion;
import House.prototype.Model.Building;
import House.prototype.Model.User;
import House.prototype.Service.DealService;
import House.prototype.Service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class UserController {
    private final UserService userService;
    private final DealService dealService;
    public UserController(UserService userService, DealService dealService){
        this.userService = userService;
        this.dealService = dealService;
    }
    @GetMapping("/")
    public String showMainPage(HttpSession session, Model model){
        User user = (User) session.getAttribute("user");
        if (user != null){
            model.addAttribute("deals", dealService.findForUser(user.getId(), 0));
        }
        return "main";
    }

    @GetMapping("/register")
    public String showRegisterForm(){
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String email,
                           HttpSession session){
        try{
            User user = userService.register(username, email, password);
            session.setAttribute("user", user);
        }catch (IllegalArgumentException e){
            return "redirect:/register?error";
        }
        return "redirect:/account";
    }

    @GetMapping("/success")
    public String successPage(){
        return "success";
    }
    @GetMapping("/login")
    public String showLoginPage(){
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session){
        try{
            User user = userService.login(username, password);
            session.setAttribute("user", user);
        }catch (IllegalArgumentException e){
            return "redirect:/login?error";
        }
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String showAccount(HttpSession session, Model model){
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null){
            return "redirect:/login";
        }
        User user = userService.getById(sessionUser.getId());
        fillProfileModel(model, user);
        model.addAttribute("myDeals", dealService.findByAuthor(user.getId()));
        return "account";
    }

    @GetMapping("/account/settings")
    public String showSettings(HttpSession session, Model model){
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null){
            return "redirect:/login";
        }
        User user = userService.getById(sessionUser.getId());
        fillProfileModel(model, user);
        return "settings";
    }

    @GetMapping("/residents")
    public String showResidents(HttpSession session, Model model){
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null){
            return "redirect:/login";
        }
        User user = userService.getById(sessionUser.getId());
        Building b = user.getBuilding();
        if (b != null){
            String streetPart = b.getStreet() != null
                    ? b.getStreet() + " " + b.getNumber()
                    : b.getNumber();
            model.addAttribute("buildingAddress", streetPart + ", " + b.getCity());
        }
        model.addAttribute("neighbours", userService.neighbours(user.getId()));
        return "residents";
    }

    @GetMapping("/residents/{id}")
    public String showResident(@PathVariable long id, HttpSession session, Model model){
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null){
            return "redirect:/login";
        }
        User resident;
        try{
            resident = userService.neighbourProfile(sessionUser.getId(), id);
        }catch (IllegalArgumentException e){
            return "redirect:/residents?error";
        }
        model.addAttribute("resident", resident);
        Building b = resident.getBuilding();
        if (b != null){
            String streetPart = b.getStreet() != null
                    ? b.getStreet() + " " + b.getNumber()
                    : b.getNumber();
            model.addAttribute("buildingAddress", streetPart + ", " + b.getCity());
        }
        return "resident";
    }

    @GetMapping("/residents/{id}/avatar")
    public ResponseEntity<byte[]> residentAvatar(@PathVariable long id, HttpSession session){
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null){
            return ResponseEntity.status(401).build();
        }
        User resident;
        try{
            resident = userService.neighbourProfile(sessionUser.getId(), id);
        }catch (IllegalArgumentException e){
            return ResponseEntity.notFound().build();
        }
        if (!resident.hasAvatar()){
            return ResponseEntity.notFound().build();
        }
        MediaType type = resident.getAvatarType() != null
                ? MediaType.parseMediaType(resident.getAvatarType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(type).body(resident.getAvatar());
    }

    private void fillProfileModel(Model model, User user){
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("hasAvatar", user.hasAvatar());
        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("phone", user.getPhone());
        model.addAttribute("bio", user.getBio());
        model.addAttribute("floor", user.getFloor());
        model.addAttribute("apartment", user.getApartment());
        Building b = user.getBuilding();
        if (b != null){
            String streetPart = b.getStreet() != null
                    ? b.getStreet() + " " + b.getNumber()
                    : b.getNumber();
            model.addAttribute("buildingAddress", streetPart + ", " + b.getCity());
        }
    }

    @PostMapping("/account/username")
    public String changeUsername(@RequestParam String username, HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null){
            return "redirect:/login";
        }
        try{
            User updated = userService.changeUsername(user.getId(), username);
            session.setAttribute("user", updated);
        }catch (IllegalArgumentException e){
            return "redirect:/account/settings?error";
        }
        return "redirect:/account";
    }

    @PostMapping("/account/profile")
    public String updateProfile(@RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String floor,
                                @RequestParam(required = false) String apartment,
                                HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null){
            return "redirect:/login";
        }
        try{
            User updated = userService.updateProfile(user.getId(), fullName, phone, bio,
                    floor, apartment);
            session.setAttribute("user", updated);
        }catch (IllegalArgumentException e){
            return "redirect:/account/settings?error";
        }
        return "redirect:/account";
    }

    @PostMapping("/account/email")
    public String changeEmail(@RequestParam String email, HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null){
            return "redirect:/login";
        }
        try{
            User updated = userService.changeEmail(user.getId(), email);
            session.setAttribute("user", updated);
        }catch (IllegalArgumentException e){
            return "redirect:/account/settings?error";
        }
        return "redirect:/account";
    }

    @PostMapping("/account/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null){
            return "redirect:/login";
        }
        try{
            userService.changePassword(user.getId(), currentPassword, newPassword);
        }catch (IllegalArgumentException e){
            return "redirect:/account/settings?error";
        }
        return "redirect:/account";
    }

    @PostMapping("/account/avatar")
    public String uploadAvatar(@RequestParam MultipartFile avatar, HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null){
            return "redirect:/login";
        }
        if (avatar == null || avatar.isEmpty()){
            return "redirect:/account/settings?error";
        }
        try{
            userService.setAvatar(user.getId(), avatar.getBytes(), avatar.getContentType());
        }catch (IllegalArgumentException | IOException e){
            return "redirect:/account/settings?error";
        }
        return "redirect:/account/settings";
    }

    @GetMapping("/account/avatar")
    public ResponseEntity<byte[]> avatar(HttpSession session){
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null){
            return ResponseEntity.notFound().build();
        }
        User user = userService.getById(sessionUser.getId());
        if (!user.hasAvatar()){
            return ResponseEntity.notFound().build();
        }
        MediaType type = user.getAvatarType() != null
                ? MediaType.parseMediaType(user.getAvatarType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(type).body(user.getAvatar());
    }

    @PostMapping("/account/address")
    public String addAddress(@ModelAttribute BuildingSuggestion suggestion,
                             HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null){
            return "redirect:/login";
        }
        try{
            userService.linkBuilding(user.getId(), suggestion);
        }catch (IllegalArgumentException e){
            return "redirect:/account/settings?error";
        }
        return "redirect:/account/settings";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/";
    }

}
