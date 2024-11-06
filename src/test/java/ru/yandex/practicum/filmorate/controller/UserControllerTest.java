package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.dao.UserDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.friend.FriendStorage;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserControllerTest {
    private final UserDbStorage userDbStorage;
    private final FriendStorage friendStorage;

    @Test
    public void testFindUserById() {
        User validUser1 = getValidUser1();
        userDbStorage.create(validUser1);

        Optional<User> userOptional = Optional.ofNullable(userDbStorage.getUserById(1L));
        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(user ->
                        assertThat(user).hasFieldOrPropertyWithValue("id", 1L)
                );
    }

    @Test
    public void testFindUnknownIdUser() {
        User validUser = getValidUser1();
        userDbStorage.create(validUser);
        User invalidUser = User.builder()
                .id(5L)
                .login("user2Login")
                .name("user2Name")
                .email("user2@mail")
                .birthday(LocalDate.parse("1997-10-20"))
                .build();
        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> userDbStorage.getUserById(invalidUser.getId())
        );
        assertEquals("User not found", thrown.getMessage());
    }

    @Test
    public void testFindAllUsers() {
        User validUser1 = userDbStorage.create(getValidUser1());
        User validUser2 = userDbStorage.create(getValidUser2());

        List<User> users = userDbStorage.findAll().stream().toList();
        assertThat(users)
                .contains(validUser1, validUser2);
    }

    @Test
    public void testUpdateUser() {
        User validUser1 = getValidUser1();
        User validUser2 = getValidUser2();
        validUser2.setId(1L);
        userDbStorage.create(validUser1);
        userDbStorage.update(validUser2);

        User user = userDbStorage.getUserById(1L);
        assertThat(user)
                .hasFieldOrPropertyWithValue("id", 1L)
                .hasFieldOrPropertyWithValue("login", "user2Login");
    }

    @Test
    public void testCreateUser() {
        User validUser1 = userDbStorage.create(getValidUser1());

        User user = userDbStorage.getUserById(1L);
        assertThat(user)
                .isNotNull()
                .isEqualTo(validUser1);
    }

    @Test
    public void testAddFriends() {
        User validUser1 = userDbStorage.create(getValidUser1());
        User validUser2 = userDbStorage.create(getValidUser2());

        friendStorage.addFriend(validUser1.getId(), validUser2.getId());

        List<User> friends = friendStorage.findAllFriends(validUser1.getId());
        assertThat(friends)
                .isNotEmpty()
                .contains(validUser2);
    }

    @Test
    public void testDeleteFromFriends() {
        User validUser1 = userDbStorage.create(getValidUser1());
        User validUser2 = userDbStorage.create(getValidUser2());

        friendStorage.addFriend(validUser1.getId(), validUser2.getId());
        friendStorage.removeFriend(validUser1.getId(), validUser2.getId());
        List<User> friends = friendStorage.findAllFriends(validUser1.getId());
        assertThat(friends)
                .isEmpty();
    }

    private User getValidUser1() {
        return User.builder()
                .login("user1Login")
                .name("user1Name")
                .email("user1@mail")
                .birthday(LocalDate.parse("1997-07-08"))
                .build();
    }

    private User getValidUser2() {
        return User.builder()
                .login("user2Login")
                .name("user2Name")
                .email("user2@mail")
                .birthday(LocalDate.parse("1997-10-20"))
                .build();
    }
}