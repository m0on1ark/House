package House.prototype.Controller;

import House.prototype.DTO.BuildingSuggestion;
import House.prototype.Exception.InvalidRequestException;
import House.prototype.Model.Building;
import House.prototype.Model.User;
import House.prototype.Security.AppUserDetails;
import House.prototype.Security.AutoLogin;
import House.prototype.Service.DealService;
import House.prototype.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    private final AutoLogin autoLogin;
    public UserController(UserService userService, DealService dealService, AutoLogin autoLogin){
        this.userService = userService;
        this.dealService = dealService;
        this.autoLogin = autoLogin;
    }

    @GetMapping("/")
    public String showMainPage(@AuthenticationPrincipal AppUserDetails principal, Model model){
        if (principal != null){
            model.addAttribute("deals", dealService.findForUser(principal.getId(), 0));
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
                           HttpServletRequest request,
                           HttpServletResponse response){
        User user;
        try{
            user = userService.register(username, email, password);
        }catch (InvalidRequestException e){
            return "redirect:/register?error";
        }
        autoLogin.login(user, request, response);
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

    @GetMapping("/account")
    public String showAccount(@AuthenticationPrincipal AppUserDetails principal, Model model){
        User user = userService.getById(principal.getId());
        fillProfileModel(model, user);
        model.addAttribute("myDeals", dealService.findByAuthor(user.getId()));
        return "account";
    }

    @GetMapping("/account/settings")
    public String showSettings(@AuthenticationPrincipal AppUserDetails principal, Model model){
        User user = userService.getById(principal.getId());
        fillProfileModel(model, user);
        return "settings";
    }

    @GetMapping("/residents")
    public String showResidents(@AuthenticationPrincipal AppUserDetails principal, Model model){
        User user = userService.getById(principal.getId());
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
    public String showResident(@PathVariable long id,
                               @AuthenticationPrincipal AppUserDetails principal, Model model){
        User resident;
        try{
            resident = userService.neighbourProfile(principal.getId(), id);
        }catch (InvalidRequestException e){
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
    public ResponseEntity<byte[]> residentAvatar(@PathVariable long id,
                                                 @AuthenticationPrincipal AppUserDetails principal){
        User resident;
        try{
            resident = userService.neighbourProfile(principal.getId(), id);
        }catch (InvalidRequestException e){
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
    public String changeUsername(@RequestParam String username,
                                 @AuthenticationPrincipal AppUserDetails principal,
                                 HttpServletRequest request,
                                 HttpServletResponse response){
        User updated = userService.changeUsername(principal.getId(), username);
        autoLogin.login(updated, request, response);
        return "redirect:/account";
    }

    @PostMapping("/account/profile")
    public String updateProfile(@RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String floor,
                                @RequestParam(required = false) String apartment,
                                @AuthenticationPrincipal AppUserDetails principal){
        userService.updateProfile(principal.getId(), fullName, phone, bio, floor, apartment);
        return "redirect:/account";
    }

    @PostMapping("/account/email")
    public String changeEmail(@RequestParam String email,
                              @AuthenticationPrincipal AppUserDetails principal){
        userService.changeEmail(principal.getId(), email);
        return "redirect:/account";
    }

    @PostMapping("/account/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @AuthenticationPrincipal AppUserDetails principal,
                                 HttpServletRequest request,
                                 HttpServletResponse response){
        User updated = userService.changePassword(principal.getId(), currentPassword, newPassword);
        autoLogin.login(updated, request, response);
        return "redirect:/account";
    }

    @PostMapping("/account/avatar")
    public String uploadAvatar(@RequestParam MultipartFile avatar,
                               @AuthenticationPrincipal AppUserDetails principal) throws IOException {
        if (avatar == null || avatar.isEmpty()){
            return "redirect:/account/settings?error";
        }
        userService.setAvatar(principal.getId(), avatar.getBytes(), avatar.getContentType());
        return "redirect:/account/settings";
    }

    @GetMapping("/account/avatar")
    public ResponseEntity<byte[]> avatar(@AuthenticationPrincipal AppUserDetails principal){
        User user = userService.getById(principal.getId());
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
                             @AuthenticationPrincipal AppUserDetails principal){
        userService.linkBuilding(principal.getId(), suggestion);
        return "redirect:/account/settings";
    }

    @ExceptionHandler({InvalidRequestException.class, IOException.class})
    public String handleInvalidRequest(){
        return "redirect:/account/settings?error";
    }

}
