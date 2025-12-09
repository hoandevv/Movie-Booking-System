package com.trainning.movie_booking_system.helper.specification;

import com.trainning.movie_booking_system.dto.request.Movie.MovieSearchFilter;
import com.trainning.movie_booking_system.entity.Movie;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class MovieSpecs {

    public static Specification<Movie> build(MovieSearchFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // keyword search (title or description)
            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String like = "%" + f.getKeyword().trim().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("title")), like),
                                cb.like(cb.lower(root.get("description")), like)
                        )
                );
            }

            // genre
            if (f.getGenres() != null && !f.getGenres().isEmpty()) {
                predicates.add(root.get("genre").in(f.getGenres()));
            }

            // language
            if (f.getLanguage() != null && !f.getLanguage().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("language")), f.getLanguage().toLowerCase()));
            }

            // status
            if (f.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), f.getStatus()));
            }

            // rating range
            if (f.getRatingMin() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), f.getRatingMin()));
            if (f.getRatingMax() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("rating"), f.getRatingMax()));

            // duration range
            if (f.getDurationMin() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("duration"), f.getDurationMin()));
            if (f.getDurationMax() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("duration"), f.getDurationMax()));

            // release date range
            if (f.getReleaseFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("releaseDate"), f.getReleaseFrom()));
            if (f.getReleaseTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("releaseDate"), f.getReleaseTo()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
