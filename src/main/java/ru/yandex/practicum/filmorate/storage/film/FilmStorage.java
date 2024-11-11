package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {

    Collection<Film> findAll();

    Film create(Film film);

    Film update(Film newFilm);

    void deleteFilmById(Long id);

    Film getFilmById(Long id);

    Collection<Film> getPopularFilms(Integer count, Integer genreId, Integer year);

    Collection<Film> getCommonFilms(Long userId, Long friendId);

    List<Film> getFilmsByDirector(Long id, String sortBy);

    List<Film> searchFilms(String query, boolean isDirector, boolean isTitle);
}
