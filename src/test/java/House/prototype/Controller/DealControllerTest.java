package House.prototype.Controller;

import House.prototype.Exception.InvalidRequestException;
import House.prototype.Model.User;
import House.prototype.Security.AppUserDetails;
import House.prototype.Security.AppUserDetailsService;
import House.prototype.Security.AutoLogin;
import House.prototype.Security.SecurityConfig;
import House.prototype.Service.BuildingService;
import House.prototype.Service.ChatService;
import House.prototype.Service.DealService;
import House.prototype.Service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class DealControllerTest {

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
        user.setPassword("irrelevant");
        return new AppUserDetails(user);
    }

    @Test
    void aRejectedClaimSendsTheResidentBackToTheDealsPage() throws Exception {
        willThrow(new InvalidRequestException("not enough units left"))
                .given(dealService).claim(anyLong(), anyLong(), anyInt());

        mvc.perform(post("/deals/1/claim")
                        .with(user(resident()))
                        .with(csrf())
                        .param("units", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/deals?error"));
    }

    @Test
    void aMissingDealImageIsA404RatherThanARedirect() throws Exception {
        given(dealService.getById(404L)).willThrow(new InvalidRequestException("Invalid"));

        mvc.perform(get("/deals/404/image").with(user(resident())))
                .andExpect(status().isNotFound());
    }

    @Test
    void aBrokenAccountDoesNotTurnTheDealsPageIntoARedirectLoop() {
        given(dealService.findForUser(anyLong(), anyDouble()))
                .willThrow(new IllegalArgumentException("Invalid"));

        assertThatThrownBy(() -> mvc.perform(get("/deals").with(user(resident()))))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }
}
