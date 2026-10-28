package com.cinema.service.impl;

import com.cinema.dto.request.MovieRequest;
import com.cinema.dto.response.MovieResponse;
import com.cinema.entity.Genre;
import com.cinema.entity.Movie;
import com.cinema.exception.BusinessLogicException;
import com.cinema.repository.GenreRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        Movie movie = new Movie();
        mapToEntity(movie, request);
        return mapToResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new BusinessLogicException("Фильм не найден"));
        mapToEntity(movie, request);
        return mapToResponse(movieRepository.save(movie));
    }

    @Override
    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new BusinessLogicException("Фильм не найден");
        }
        movieRepository.deleteById(id);
    }

    @Override
    public MovieResponse getMovieById(Long id) {
        return movieRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new BusinessLogicException("Фильм не найден"));
    }

    @Override
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> searchMovies(String query) {
        // Используем Derived Query из репозитория
        return movieRepository.findByTitleContainingIgnoreCase(query).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Приватный метод маппинга Request -> Entity
    private void mapToEntity(Movie movie, MovieRequest request) {
        movie.setTitle(request.getTitle());
        movie.setDuration(request.getDuration());
        movie.setAgeRating(request.getAgeRating());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setEndDate(request.getEndDate());
        movie.setDescription(request.getDescription());
        movie.setPosterUrl(request.getPosterUrl());

        // Обработка жанров: загружаем из БД по ID
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            Set<Genre> genres = new HashSet<>(genreRepository.findAllById(request.getGenreIds()));
            if (genres.size() != request.getGenreIds().size()) {
                throw new BusinessLogicException("Один или несколько жанров не найдены");
            }
            movie.setGenres(genres);
        } else {
            movie.setGenres(new HashSet<>());
        }
    }

    // Приватный метод маппинга Entity -> Response
    private MovieResponse mapToResponse(Movie movie) {
        Set<String> genreNames = movie.getGenres().stream()
                .map(Genre::getName)
                .collect(Collectors.toSet());

        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .duration(movie.getDuration())
                .ageRating(movie.getAgeRating())
                .releaseDate(movie.getReleaseDate())
                .posterUrl(movie.getPosterUrl())
                .genres(genreNames)
                .build();
    }
}