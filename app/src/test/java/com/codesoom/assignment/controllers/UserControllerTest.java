package com.codesoom.assignment.controllers;

import com.codesoom.assignment.application.AuthenticationService;
import com.codesoom.assignment.application.UserService;
import com.codesoom.assignment.domain.User;
import com.codesoom.assignment.dto.UserModificationData;
import com.codesoom.assignment.dto.UserRegistrationData;
import com.codesoom.assignment.errors.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final Long MY_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long NOT_EXISTS_USER_ID = 100L;
    private static final Long ADMIN_USER_ID = 1000L;



    private static final String MY_TOKEN = "eyJhbGciOiJIUzI1NiJ9." +
            "eyJ1c2VySWQiOjF9.ZZ3CUl0jxeLGvQ1Js5nG2Ty5qGTlqai5ubDMXZOdaDk";

    private static final String OTHER_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjk5fQ.kbxbENfC5YoQIOGG87WLsStU38s_G_Nebr73RcBotEY";
    private static final String ADMIN_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEwMDB9.nnjhgy2R3Qo48tUtI-ib-D-Aqjfz4338xMhAHg2OFxA";


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(MY_USER_ID)
                .name("jinny")
                .email("test@mail.com")
                .roles(Collections.singletonList("USER"))
                .build();

        User otherUser = User.builder()
                .id(OTHER_USER_ID)
                .name("jinny")
                .email("test@mail.com")
                .roles(Collections.singletonList("USER"))
                .build();

        User adminUser = User.builder()
                .id(ADMIN_USER_ID)
                .name("admin")
                .email("test@mail.com")
                .roles(Collections.singletonList("ADMIN"))
                .build();


//        given(userService.findUser(OTHER_USER_ID)).willReturn(otherUser);
//        given(userService.findUser(ADMIN_USER_ID)).willReturn(adminUser);
        given(userService.registerUser(any(UserRegistrationData.class)))
                .will(invocation -> {
                    UserRegistrationData registrationData = invocation.getArgument(0);
                    return User.builder()
                            .id(13L)
                            .email(registrationData.getEmail())
                            .name(registrationData.getName())
                            .build();
                });


        given(userService.updateUser(eq(MY_USER_ID), any(UserModificationData.class)))
                .will(invocation -> {
                    Long id = invocation.getArgument(0);
                    UserModificationData modificationData =
                            invocation.getArgument(1);
                    return User.builder()
                            .id(id)
                            .email("tester@example.com")
                            .name(modificationData.getName())
                            .build();
                });

        given(userService.updateUser(eq(NOT_EXISTS_USER_ID), any(UserModificationData.class)))
                .willThrow(new UserNotFoundException(NOT_EXISTS_USER_ID));

        given(userService.deleteUser(NOT_EXISTS_USER_ID))
                .willThrow(new UserNotFoundException(NOT_EXISTS_USER_ID));

        given(authenticationService.parseToken(MY_TOKEN)).willReturn(MY_USER_ID);
        given(authenticationService.parseToken(OTHER_TOKEN)).willReturn(OTHER_USER_ID);
        given(authenticationService.parseToken(ADMIN_TOKEN)).willReturn(ADMIN_USER_ID);
        given(authenticationService.getUserRoles(MY_USER_ID)).willReturn(Collections.singletonList("USER"));
        given(authenticationService.getUserRoles(OTHER_USER_ID)).willReturn(Collections.singletonList("USER"));
        given(authenticationService.getUserRoles(ADMIN_USER_ID)).willReturn(Collections.singletonList("ADMIN"));
    }

    @Test
    void registerUserWithValidAttributes() throws Exception {
        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tester@example.com\"," +
                                "\"name\":\"Tester\",\"password\":\"test\"}")
        )
                .andExpect(status().isCreated())
                .andExpect(content().string(
                        containsString("\"id\":13")
                ))
                .andExpect(content().string(
                        containsString("\"email\":\"tester@example.com\"")
                ))
                .andExpect(content().string(
                        containsString("\"name\":\"Tester\"")
                ));

        verify(userService).registerUser(any(UserRegistrationData.class));
    }

    @Test
    void registerUserWithInvalidAttributes() throws Exception {
        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserWithValidAttributes() throws Exception {
        mockMvc.perform(
                patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TEST\",\"password\":\"test\"}")
                        .header("Authorization", "Bearer " + MY_TOKEN)
        )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("\"id\":1")
                ))
                .andExpect(content().string(
                        containsString("\"name\":\"TEST\"")
                ));

        verify(userService).updateUser(eq(MY_USER_ID), any(UserModificationData.class));
    }

    @Test
    @DisplayName("타인토큰_유저정보수정_Forbidden")
    void updateUserWithOtherToken() throws Exception {
        mockMvc.perform(
                        patch("/users/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"TEST\",\"password\":\"test\"}")
                                .header("Authorization", "Bearer " + OTHER_TOKEN)
                )
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUser(eq(MY_USER_ID), any(UserModificationData.class));
    }

    @Test
    void updateUserWithInvalidAttributes() throws Exception {
        mockMvc.perform(
                patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"password\":\"\"}")
                        .header("Authorization", "Bearer " + MY_TOKEN)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserWithNotExsitedId() throws Exception {
        mockMvc.perform(
                patch("/users/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TEST\",\"password\":\"TEST\"}")
                        .header("Authorization", "Bearer " + MY_TOKEN)
        )
                .andExpect(status().isForbidden());

        verify(userService, never())
                .updateUser(eq(NOT_EXISTS_USER_ID), any(UserModificationData.class));
    }

    @Test
    void destroyWithExistedId() throws Exception {
        mockMvc.perform(delete("/users/1")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void destroyWithoutToken() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void destroyWithNotAdminToken() throws Exception {
        mockMvc.perform(delete("/users/1000")
                        .header("Authorization", "Bearer " + MY_TOKEN))
                .andExpect(status().isForbidden());
    }
}
