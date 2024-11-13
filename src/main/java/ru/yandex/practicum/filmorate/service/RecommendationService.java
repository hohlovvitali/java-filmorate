package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.LikesDbStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final LikesDbStorage likeStorage;
    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    public RecommendationService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                                 @Qualifier("userDbStorage") UserStorage userStorage, LikesDbStorage likeStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.likeStorage = likeStorage;
    }

    public Set<Film> getRecommendationFilms(Long userId) {
        log.info("Получение рекомендаций для пользователся с id = {}", userId);
        //Получение списка пользователей удаление, пользователя для которого составляется рекомендация
        List<Long> usersIdList = new ArrayList<>(userStorage.findAll().stream().map(User::getId).toList());
        usersIdList.remove(userId);

        // Создание и заполнение map id-пользователей и множеством id-фильмов, которым ставили лайк
        Map<Long, Set<Long>> usersFilms = new HashMap<>();
        for (Long id : usersIdList) {
            if (!likeStorage.getUsersFilmLikes(id).isEmpty()) {
                usersFilms.put(id, likeStorage.getUsersFilmLikes(id));
            }
        }

        // Получение множества id-фильмов для пользователя
        Set<Long> userFilmsId = likeStorage.getUsersFilmLikes(userId);
        if (userFilmsId.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        // Поиск id-пользователя с максимальным количеством совпадений с рекомендуемым пользователем
        Long userIdWithTopFreq = -1L;
        int topFreq = 0;

        for (Long id : usersFilms.keySet()) {
            Set<Long> filmsId = new HashSet<>(usersFilms.get(id));
            filmsId.retainAll(userFilmsId);
            int countFreq = filmsId.size();
            if (countFreq > topFreq) {
                topFreq = countFreq;
                userIdWithTopFreq = id;
            }
        }

        if (topFreq == 0) {
            return Collections.EMPTY_SET;
        }

        // Получение множества фильмов пользователя с максимальным количеством совпадений
        // и удаление совпадений со множеством фильмов рекомендуемого
        Set<Long> filmsId = usersFilms.get(userIdWithTopFreq);
        filmsId.removeAll(userFilmsId);

        return filmsId.stream().map(filmStorage::getFilmById).collect(Collectors.toSet());
    }

//    public Set<Film> getRecommendationFilms(Long userId) {
//        log.info("Получение рекомендаций для пользователся с id = {}", userId);
//        Map<Long, List<Long>> usersFilms = new HashMap<>();
//
//        List<User> userList = (List<User>) userStorage.findAll();
//
//        for (User user : userList) {
//            usersFilms.put(user.getId(), filmStorage.getFilmsIdByUserId(user.getId()));
//        }
//
//        long maxMatches = 0;
//        Set<Long> similar = new HashSet<>();
//
//        for (Long id : usersFilms.keySet()) {
//            if (Objects.equals(id, userId)) continue;
//
//            long numberOfMatches = usersFilms.get(userId).stream()
//                    .filter(filmId -> usersFilms.get(userId).contains(filmId)).count();
//
//            if (numberOfMatches == maxMatches & numberOfMatches != 0) {
//                similar.add(id);
//            }
//
//            if (numberOfMatches > maxMatches) {
//                maxMatches = numberOfMatches;
//                similar = new HashSet<>();
//                similar.add(id);
//            }
//        }
//
//        if (maxMatches == 0) {
//            return new HashSet<>();
//        }
//
//        return similar.stream().flatMap(id -> filmStorage.getFilmsIdByUserId(id).stream())
//                .filter(filmId -> !usersFilms.get(userId).contains(filmId))
//                .map(filmStorage::getFilmById)
//                .collect(Collectors.toSet());
//    }
}
