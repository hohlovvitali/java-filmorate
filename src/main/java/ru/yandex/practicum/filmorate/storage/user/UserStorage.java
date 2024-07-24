package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;

public interface UserStorage {
    User create(User user);

    void addFriendToUser(Long idUser, Long idFriend);

    void deleteFriendFromUser(Long idUser, Long idFriend);

    void delete(Long id);

    User update(User user);

    Collection<User> getFriends(Long id);

    Collection<User> findAll();

    User getUserById(Long id);
}
