package vn.edu.iuh.fit.service;

import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entity.Genre;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertGenreRequest;
import vn.edu.iuh.fit.repository.GenreRepository;
import vn.edu.iuh.fit.repository.MovieRepository;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreRepository genreRepository;
    private final Slugify slugify;
    private final MovieRepository movieRepository;

    public List<Genre> getAllGenres() {
        return genreRepository.findAll(Sort.by("id").descending());
    }

    public Genre saveGenre(UpsertGenreRequest request) {
        // check tag name is exist
        if (genreRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Thể loại đã tồn tại");
        }

        Genre genre = new Genre();
        genre.setName(request.getName());
        genre.setSlug(slugify.slugify(request.getName()));
        genreRepository.save(genre);
        return genre;
    }

    public Genre updateGenre(Integer id, UpsertGenreRequest genre) {
        Genre existingGenre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại có id = " + id));

        // Kiểm tra tên thể loại đã tồn tại hay chưa. Nếu đã tồn tại và không phải là thể loại cần update thì throw exception
        if (genreRepository.findByName(genre.getName()).isPresent() && !Objects.equals(existingGenre.getName(), genre.getName())) {
            throw new BadRequestException("Thể loại đã tồn tại");
        }

        existingGenre.setName(genre.getName());
        existingGenre.setSlug(slugify.slugify(genre.getName()));
        return genreRepository.save(existingGenre);
    }

    public void deleteGenre(Integer id) {
        Genre existingGenre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại có id = " + id));

        // Đếm số phim có thể loại này
        long count = movieRepository.countByGenres_Id(existingGenre.getId());
        if (count > 0) {
            throw new BadRequestException("Không thể xóa thể loại này vì có " + count + " phim thuộc thể loại này");
        }

        genreRepository.deleteById(id);
    }
}
