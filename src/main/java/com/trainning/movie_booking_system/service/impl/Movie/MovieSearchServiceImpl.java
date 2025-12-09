package com.trainning.movie_booking_system.service.impl.Movie;

import com.trainning.movie_booking_system.dto.request.Movie.MovieSearchFilter;
import com.trainning.movie_booking_system.dto.response.Movie.MovieResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Movie;
import com.trainning.movie_booking_system.helper.specification.MovieSpecs;
import com.trainning.movie_booking_system.mapper.MovieMapper;
import com.trainning.movie_booking_system.repository.MovieRepository;
import com.trainning.movie_booking_system.service.Movie.MovieSearchService;
import com.trainning.movie_booking_system.utils.enums.MovieSortField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieSearchServiceImpl implements MovieSearchService {

    private final MovieRepository movieRepository;

    /**
     * Search movies based on filters with pagination
     *
     * @param filter the movie search filter
     * @return a paginated response of movies matching the search criteria
     */
    @PreAuthorize("permitAll()")
    @Override
    public PageResponse<?> search(MovieSearchFilter filter) {
        log.info("[MOVIE SERVICE] - Search movies with filter: {}", filter);

        int page = filter.getPage() == null || filter.getPage() < 0 ? 0 : filter.getPage();
        int size = filter.getSize() == null || filter.getSize() <= 0 ? 12 : Math.min(filter.getSize(), 50);
        String sortField = MovieSortField.safe(filter.getSortBy());
        Sort sort = Sort.by(filter.getDirection(), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Movie> spec = MovieSpecs.build(filter);
        Page<MovieResponse> result = movieRepository.findAll(spec, pageable)
                .map(MovieMapper::toMovieResponse);

        return PageResponse.of(result);
    }

}
