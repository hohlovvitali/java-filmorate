package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {
    private UserController userController;

    @BeforeEach
    public void beforeEach() {
        userController = new UserController(new UserService(new InMemoryUserStorage()));
    }

    @Test
    void findAll() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        User user2 = User.builder()
                .email("user2@.com")
                .login("user2login")
                .name("user2")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        userController.create(user1);
        userController.create(user2);

        Collection<User> usersCollectionFromMethod = userController.findAll();
        Collection<User> usersCollection = new ArrayList<>();
        usersCollection.add(user1);
        usersCollection.add(user2);

        assertNotNull(usersCollectionFromMethod, "Список пользователей не возращается");
        assertEquals(2, usersCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(usersCollection, usersCollectionFromMethod.stream().toList(), "Возращается не тот список");
    }

    @Test
    void createTest() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        userController.create(user1);

        Collection<User> usersCollectionFromMethod = userController.findAll();

        assertNotNull(usersCollectionFromMethod, "Список фильмов не возращается");
        assertEquals(1, usersCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(user1, usersCollectionFromMethod.stream().toList().getFirst(), "Возращается не того пользователя");
    }

    @Test
    void createWithEmptyEmail() {
        User user1 = User.builder()
                .email("")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(user1)
        );

        Assertions.assertEquals("E-mail должен быть указан", ex.getMessage());
    }

    @Test
    void createUsersWithoutEmailSymbols() {
        User user1 = User.builder()
                .email("user1.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(user1)
        );

        Assertions.assertEquals("E-mail должен содержать знак @", ex.getMessage());
    }

    @Test
    void createUsersWithDuplicateEmail() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        User user2 = User.builder()
                .email("user1@.com")
                .login("user2login")
                .name("user2")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        userController.create(user1);

        DuplicatedDataException ex = Assertions.assertThrows(
                DuplicatedDataException.class,
                generateCreateException(user2)
        );

        Assertions.assertEquals("E-mail уже используется", ex.getMessage());
    }

    @Test
    void createWithEmptyLogin() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(user1)
        );

        Assertions.assertEquals("Логин должен быть указан и не содержать пробелы", ex.getMessage());
    }

    @Test
    void createLoginWithSpace() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1 login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(user1)
        );

        Assertions.assertEquals("Логин должен быть указан и не содержать пробелы", ex.getMessage());
    }

    @Test
    void createUserWithFutureBirthday() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2100-01-01"))
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(user1)
        );

        Assertions.assertEquals("Дата рождения не может быть в будущем", ex.getMessage());
    }

    @Test
    void createUserWithNowBirthday() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.now())
                .build();

        userController.create(user1);

        Collection<User> usersCollectionFromMethod = userController.findAll();

        assertNotNull(usersCollectionFromMethod, "Список фильмов не возращается");
        assertEquals(1, usersCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(user1.getBirthday(), LocalDate.now(), "Возращается не та дата рождения");
    }

    @Test
    void createUserWithEmptyName() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .birthday(LocalDate.now())
                .build();

        userController.create(user1);

        Collection<User> usersCollectionFromMethod = userController.findAll();

        assertNotNull(usersCollectionFromMethod, "Список фильмов не возращается");
        assertEquals(1, usersCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(user1.getLogin(), usersCollectionFromMethod.stream().toList().getFirst().getName(), "Имя неверно заполняется логином");
    }

    @Test
    void updateTest() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        userController.create(user1);

        User userNew = User.builder()
                .id(1L)
                .email("user1new@.com")
                .login("user1loginNew")
                .name("user1new")
                .birthday(LocalDate.parse("2000-01-12"))
                .build();
        userController.update(userNew);

        Collection<User> usersCollectionFromMethod = userController.findAll();

        assertNotNull(usersCollectionFromMethod, "Список фильмов не возращается");
        assertEquals(1, usersCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(userNew, usersCollectionFromMethod.stream().toList().getFirst(), "Возращается неправильно обновленный пользователь");
    }

    @Test
    void updateWithEmptyId() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateUpdateException(user1)
        );

        Assertions.assertEquals("Id должен быть указан", ex.getMessage());
    }

    @Test
    void updateWithNonExistentId() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        userController.create(user1);
        user1.setId(5L);

        NotFoundException ex = Assertions.assertThrows(
                NotFoundException.class,
                generateUpdateException(user1)
        );

        Assertions.assertEquals("Пользователь с id = 5 не найден", ex.getMessage());
    }

    @Test
    void updateEmailWithoutSymbol() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        userController.create(user1);

        User userDuplicate = User.builder()
                .id(1L)
                .email("user1.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateUpdateException(userDuplicate)
        );

        Assertions.assertEquals("E-mail должен содержать знак @", ex.getMessage());
    }

    @Test
    void updateWithDuplicateUserEmail() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        User user2 = User.builder()
                .email("user2@.com")
                .login("user2login")
                .name("user2")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        userController.create(user1);
        userController.create(user2);

        User userDuplicate = User.builder()
                .id(1L)
                .email("user2@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();

        DuplicatedDataException ex = Assertions.assertThrows(
                DuplicatedDataException.class,
                generateUpdateException(userDuplicate)
        );

        Assertions.assertEquals("Этот e-mail уже используется", ex.getMessage());
    }

    @Test
    void addFriendTest() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user1);

        User user2 = User.builder()
                .email("user2@.com")
                .login("user2login")
                .name("user2")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user2);

        userController.addFriend(user1.getId(), user2.getId());
        Collection<User> usersFriends = userController.getFriends(user1.getId());

        assertNotNull(usersFriends, "Список друзей не возращается");
        assertEquals(1, usersFriends.size(), "Неверный размер возращаемого списка");
        assertEquals(user2.getId(), usersFriends.stream().toList().getFirst().getId(), "Возращается неправильный id друга");

        usersFriends = userController.getFriends(user2.getId());

        assertNotNull(usersFriends, "Список друзей не возращается");
        assertEquals(1, usersFriends.size(), "Неверный размер возращаемого списка");
        assertEquals(user1.getId(), usersFriends.stream().toList().getFirst().getId(), "Возращается неправильный id друга");
    }

    @Test
    void addFriendWithNonExistentId() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user1);

        User user2 = User.builder()
                .email("user2@.com")
                .login("user2login")
                .name("user2")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user2);

        NotFoundException ex = Assertions.assertThrows(
                NotFoundException.class,
                generateAddFriendException(user1.getId(), 3L)
        );

        Assertions.assertEquals("Пользователь с id = 3 не найден", ex.getMessage());
    }

    @Test
    void addFriendWitIdLess0() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user1);

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateAddFriendException(user1.getId(), -1L)
        );

        Assertions.assertEquals("Id должен быть больше 0", ex.getMessage());
    }

    @Test
    void addFriendWitIdNull() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user1);

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateAddFriendException(user1.getId(), null)
        );

        Assertions.assertEquals("Id должен быть указан", ex.getMessage());
    }

    @Test
    void getCommonFriendTest() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user1);

        User user2 = User.builder()
                .email("user2@.com")
                .login("user2login")
                .name("user2")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user2);

        User user3 = User.builder()
                .email("user3@.com")
                .login("user3login")
                .name("user3")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user3);

        userController.addFriend(user1.getId(), user2.getId());
        userController.addFriend(user3.getId(), user2.getId());
        Collection<User> usersFriends = userController.getCommonFriends(user1.getId(), user3.getId());

        assertNotNull(usersFriends, "Список общих друзей не возращается");
        assertEquals(1, usersFriends.size(), "Неверный размер возращаемого списка");
        assertEquals(user2.getId(), usersFriends.stream().toList().getFirst().getId(), "Возращается неправильный id общего друга");
    }

    @Test
    void deleteFriendTest() {
        User user1 = User.builder()
                .email("user1@.com")
                .login("user1login")
                .name("user1")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user1);

        User user2 = User.builder()
                .email("user2@.com")
                .login("user2login")
                .name("user2")
                .birthday(LocalDate.parse("2000-01-01"))
                .build();
        userController.create(user2);

        userController.addFriend(user1.getId(), user2.getId());
        userController.deleteFriend(user1.getId(), user2.getId());
        Collection<User> usersFriends = userController.getFriends(user1.getId());

        assertNotNull(usersFriends, "Список друзей не возращается");
        assertEquals(0, usersFriends.size(), "Неверный размер возращаемого списка");

        usersFriends = userController.getFriends(user2.getId());

        assertNotNull(usersFriends, "Список друзей не возращается");
        assertEquals(0, usersFriends.size(), "Неверный размер возращаемого списка");
    }

    private Executable generateAddFriendException(Long userId, Long friendId) {
        return () -> userController.addFriend(userId, friendId);
    }

    private Executable generateCreateException(User user) {
        return () -> userController.create(user);
    }

    private Executable generateUpdateException(User user) {
        return () -> userController.update(user);
    }
}