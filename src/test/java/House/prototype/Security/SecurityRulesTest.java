package House.prototype.Security;

import House.prototype.Model.Deal;
import House.prototype.Model.User;
import House.prototype.Service.BuildingService;
import House.prototype.Service.ChatService;
import House.prototype.Service.DealService;
import House.prototype.Service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class SecurityRulesTest {

    private static final String RAW_PASSWORD = "correct-horse";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private DealService dealService;
    @MockitoBean
    private ChatService chatService;
    @MockitoBean
    private BuildingService buildingService;
    @MockitoBean
    private AutoLogin autoLogin;
    @MockitoBean
    private AppUserDetailsService userDetailsService;

    private static AppUserDetails resident() {
        User user = new User();
        user.setId(7L);
        user.setUsername("anna");
        user.setPassword(new BCryptPasswordEncoder().encode(RAW_PASSWORD));
        return new AppUserDetails(user);
    }

    @Test
    void loginPageIsReachableWithoutAnAccount() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void anonymousVisitorIsSentToLoginInsteadOfTheDealsPage() throws Exception {
        mvc.perform(get("/deals"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void anonymousVisitorCannotReadAnotherResidentsChatImage() throws Exception {
        mvc.perform(get("/chat/building/image/1"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void signedInResidentReachesTheSameProtectedUrl() throws Exception {
        Deal deal = new Deal();
        deal.setImage(new byte[]{1, 2, 3});
        deal.setImageType(MediaType.IMAGE_PNG_VALUE);
        given(dealService.getById(1L)).willReturn(deal);

        mvc.perform(get("/deals/1/image").with(user(resident())))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousVisitorCannotReadADealImage() throws Exception {
        mvc.perform(get("/deals/1/image"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void formPostWithoutACsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/deals")
                        .with(user(resident()))
                        .param("productName", "Water")
                        .param("storeName", "Zabka")
                        .param("totalUnits", "6")
                        .param("authorUnits", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    void formPostWithACsrfTokenGoesThrough() throws Exception {
        mvc.perform(post("/deals")
                        .with(user(resident()))
                        .with(csrf())
                        .param("productName", "Water")
                        .param("storeName", "Zabka")
                        .param("totalUnits", "6")
                        .param("authorUnits", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/deals"));
    }

    @Test
    void correctCredentialsCreateAnAuthenticatedSession() throws Exception {
        given(userDetailsService.loadUserByUsername("anna")).willReturn(resident());

        mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "anna")
                        .param("password", RAW_PASSWORD))
                .andExpect(redirectedUrl("/account"))
                .andExpect(authenticated().withUsername("anna"));
    }

    @Test
    void aWrongPasswordLeavesTheVisitorAnonymous() throws Exception {
        given(userDetailsService.loadUserByUsername("anna")).willReturn(resident());

        mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "anna")
                        .param("password", "not-the-password"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void anUnknownUsernameLeavesTheVisitorAnonymous() throws Exception {
        given(userDetailsService.loadUserByUsername(anyString()))
                .willThrow(new UsernameNotFoundException("Unknown user"));

        mvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "ghost")
                        .param("password", RAW_PASSWORD))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }
}
