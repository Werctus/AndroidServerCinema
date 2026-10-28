package com.cinema.service;

import com.cinema.dto.request.MovieRequest;
import com.cinema.dto.response.MovieResponse;
import java.util.List;

public interface MovieService {
    MovieResponse createMovie(MovieRequest request);
    MovieResponse updateMovie(Long id, MovieRequest request);
    void deleteMovie(Long id);
    MovieResponse getMovieById(Long id);
    List<MovieResponse> getAllMovies();
    List<MovieResponse> searchMovies(String query); // Поиск по названию или жанру
}