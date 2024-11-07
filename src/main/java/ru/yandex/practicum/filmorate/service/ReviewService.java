package ru.yandex.practicum.filmorate.service;


import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.ReviewDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.GradeReview;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ReviewService {
    private final ReviewDbStorage reviewDbStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private static final String gradeLike = "Like";
    private static final String gradeDislike = "Dislike";

    public ReviewService(@Qualifier("userDbStorage") UserStorage userStorage, ReviewDbStorage reviewDbStorage,
                         @Qualifier("filmDbStorage") FilmStorage filmStorage) {
        this.reviewDbStorage = reviewDbStorage;
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
    }

    public Review create(Review review) {
        log.info("Создаем новый отзыв");
        return reviewDbStorage.create(review);
    }

    public Review getReviewById(Long id) {
        log.info("Получаем отзыв с id = " + id);
        Optional<Review> reviewOptional = reviewDbStorage.getReviewById(id);
        log.trace("Проверяем, что такой отзыв существует");
        if (reviewOptional.isEmpty()) {
            log.warn("Отзыв с id = " + id + " не найден");
            throw new NotFoundException("Отзыва с id = " + id + " не существует");
        }
        log.trace("Отзыв найден");
        return reviewOptional.get();
    }

    public Review updateReview(Review review) {
        log.info("Обновляем отзыв");
        Review rev = validationForUpdate(review);
        review.setUseful(rev.getUseful());
        return reviewDbStorage.update(review);
    }

    public void deleteReview(Long id) {
        log.info("Удаляем отзыв");
        if (!reviewDbStorage.deleteReview(id)) {
            log.warn("Отзыва с id = " + id + " не существует");
            throw new NotFoundException("Отзыв с id = " + id + " не найден");
        }
    }

    public List<Review> getAllReviews(Long filmId, int count) {
        if (filmId == null) {
            return reviewDbStorage.getAll(count);
        }
        filmStorage.getFilmById(filmId);
        return reviewDbStorage.getFilmReviews(filmId, count);
    }

    public void addLikeReview(Long reviewId, Long userId) {
        log.info("Пользователь с id = " + userId + " ставит лайк отзыву с id = " + reviewId);
        validationForGradeReview(reviewId, userId);
        log.trace("Проверяем оставлял ли пользователь оценку данному отзыву");
        Optional<GradeReview> gradeReviewOptional = reviewDbStorage.getGrandReview(reviewId, userId);
        if (gradeReviewOptional.isEmpty()) {
            log.trace("Пользоваетль не оценивал данный отзыв");
            log.trace("Увеличиваем рейтинг полезности на один");
            reviewDbStorage.increaseUseful(1, reviewId);
            reviewDbStorage.addGradeReview(reviewId, userId, gradeLike);
            return;
        }
        GradeReview gradeReview = gradeReviewOptional.get();
        if (gradeReview.getGrade().equals(gradeLike)) {
            log.warn("Пользователь уже поставил положительную оценку данному отзыву");
            throw new ValidationException("Нельзя поставить лайк отзыву дважды");
        }
        if (gradeReview.getGrade().equals(gradeDislike)) {
            log.trace("Меняем отрицательную оценку на положительную");
            log.trace("Увеличиваем рейтинг полезности на два");
            reviewDbStorage.increaseUseful(2, reviewId);
            reviewDbStorage.updateGrandReview(gradeLike, reviewId, userId);
        }
    }

    public void deleteLikeReview(Long reviewId, Long userId) {
        log.info("Пользователь с id = " + userId + " удаляет лайк отзыву с id = " + reviewId);
        validationForGradeReview(reviewId, userId);
        log.trace("Проверяем оставлял ли пользователь оценку данному отзыву");
        Optional<GradeReview> gradeReviewOptional = reviewDbStorage.getGrandReview(reviewId, userId);
        if (gradeReviewOptional.isEmpty() || gradeReviewOptional.get().getGrade().equals(gradeDislike)) {
            log.warn("Пользователь не ставил лайк");
            throw new ValidationException("Пользователь с id = " + userId + " не ставил лайк отзыву с id = " + reviewId);
        }
        if (gradeReviewOptional.get().getGrade().equals(gradeLike)) {
            log.trace("Уменьшаем рейтинг полезности на один");
            reviewDbStorage.decreaseUseful(1, reviewId);
            reviewDbStorage.deleteGradeReview(reviewId, userId);
        }
    }

    public void addDislikeReview(Long reviewId, Long userId) {
        log.info("Пользователь с id = " + userId + " ставит дизлайк отзыву с id = " + reviewId);
        validationForGradeReview(reviewId, userId);
        log.trace("Проверяем оставлял ли пользователь оценку данному отзыву");
        Optional<GradeReview> gradeReviewOptional = reviewDbStorage.getGrandReview(reviewId, userId);
        if (gradeReviewOptional.isEmpty()) {
            log.trace("Пользоваетль не оценивал данный отзыв");
            log.trace("Уменьшаем рейтинг полезности на один");
            reviewDbStorage.decreaseUseful(1, reviewId);
            reviewDbStorage.addGradeReview(reviewId, userId, gradeDislike);
            return;
        }
        GradeReview gradeReview = gradeReviewOptional.get();
        if (gradeReview.getGrade().equals(gradeLike)) {
            log.trace("Меняем положительную оценку на отрицательную");
            reviewDbStorage.decreaseUseful(2, reviewId);
            reviewDbStorage.updateGrandReview(gradeDislike, reviewId, userId);
            return;
        }
        if (gradeReview.getGrade().equals(gradeDislike)) {
            log.warn("Пользователь уже поставил отрицательную оценку данному отзыву");
            throw new ValidationException("Нельзя поставить дизлайк отзыву дважды");
        }
    }

    public void deleteDislikeReview(Long reviewId, Long userId) {
        log.info("Пользователь с id = " + userId + " удаляет дизлайк отзыву с id = " + reviewId);
        validationForGradeReview(reviewId, userId);
        log.trace("Проверяем оставлял ли пользователь оценку данному отзыву");
        Optional<GradeReview> gradeReviewOptional = reviewDbStorage.getGrandReview(reviewId, userId);
        if (gradeReviewOptional.isEmpty() || gradeReviewOptional.get().getGrade().equals(gradeLike)) {
            log.warn("Пользователь не ставил дизлайк");
            throw new ValidationException("Пользователь с id = " + userId + " не ставил дизлайк отзыву с id = " + reviewId);
        }
        if (gradeReviewOptional.get().getGrade().equals(gradeDislike)) {
            log.trace("Увеличиваем рейтинг полезности на один");
            reviewDbStorage.increaseUseful(1, reviewId);
            reviewDbStorage.deleteGradeReview(reviewId, userId);
        }
    }

    private void validationForGradeReview(Long reviewId, Long userId) {
        log.info("Пользователь с id = " + userId + " ставит лайк отзыву с id = " + reviewId);
        log.trace("Проверяем существование отзыва");
        if (reviewDbStorage.getReviewById(reviewId).isEmpty()) {
            log.warn("Отзыва с id = " + reviewId + " не существует");
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден");
        }
        log.trace("Отзыв существует");
        log.trace("Проверяем существование пользователя");
        userStorage.getUserById(userId);
    }


    private Review validationForUpdate(Review review) {
        log.trace("Проверка индефикатора на null");
        if (review.getReviewId() == null) {
            log.warn("Данные не обновленны: Для обновленние нужно указать id фильма");
            throw new ValidationException("Id должен быть указан");
        }
        Optional<Review> reviewOptional = reviewDbStorage.getReviewById(review.getReviewId());
        log.trace("Проверяем, что такой отзыв существует");
        if (reviewOptional.isEmpty()) {
            log.warn("Отзыв с id = " + review.getReviewId() + " не найден");
            throw new NotFoundException("Отзыва с id = " + review.getReviewId() + " не существует");
        }
        log.trace("Отзыв найден");
        return reviewOptional.get();
    }
}
