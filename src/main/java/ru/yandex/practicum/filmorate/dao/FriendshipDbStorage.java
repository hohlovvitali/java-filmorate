package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.mappers.UserMapper;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.friend.FriendStorage;

import java.util.List;

@RequiredArgsConstructor
@Component
public class FriendshipDbStorage implements FriendStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addFriend(Long userId, Long friendId) {
        String sql = "INSERT INTO friendships(requester_id, addressee_id) VALUES (?,?)";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendships WHERE requester_id =? AND addressee_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public List<User> findAllFriends(Long id) {
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM friendships AS f " +
                "INNER JOIN users AS u ON u.user_id = f.addressee_id " +
                "WHERE f.requester_id = ? " +
                "ORDER BY u.user_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new UserMapper().mapRow(rs, rowNum), id);
    }

    @Override
    public List<User> findCommonFriends(Long id, Long otherId) {
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM friendships AS f " +
                "INNER JOIN friendships fr ON fr.addressee_id = f.addressee_id " +
                "INNER JOIN users u ON u.user_id = fr.addressee_id " +
                "WHERE f.requester_id = ? AND fr.requester_id = ? " +
                "AND f.addressee_id <> fr.requester_id AND fr.addressee_id <> f.requester_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new UserMapper().mapRow(rs, rowNum), id, otherId);
    }
}
