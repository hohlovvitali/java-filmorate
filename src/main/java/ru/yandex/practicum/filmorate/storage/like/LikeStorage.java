package ru.yandex.practicum.filmorate.storage.like;

import java.util.Set;

public interface LikeStorage {
    void addLike(Long id, Long userId);

    void removeLike(Long id, Long userId);

    Set<Long> getLikes(Long filmId);
}
