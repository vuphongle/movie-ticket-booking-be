package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.controller.UpsertActorRequest;
import vn.edu.iuh.fit.entity.Actor;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.repository.ActorRepository;
import vn.edu.iuh.fit.repository.MovieRepository;
import vn.edu.iuh.fit.utils.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActorService {
    private final ActorRepository actorRepository;
    private final MovieRepository movieRepository;

    public List<Actor> getAllActors() {
        return actorRepository.findAll(Sort.by("createdAt").descending());
    }

    public Actor getActorById(Integer id) {
        return actorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy diễn viên có id = " + id));
    }

    public Actor saveActor(UpsertActorRequest request) {
        String avatarUrl = (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) 
                ? request.getAvatar() 
                : StringUtils.generateLinkImage(request.getName());
                
        Actor actor = Actor.builder()
                .name(request.getName())
                .description(request.getDescription())
                .birthday(request.getBirthday())
                .avatar(avatarUrl)
                .build();
        return actorRepository.save(actor);
    }

    public Actor updateActor(Integer id, UpsertActorRequest request) {
        Actor existingActor = actorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy diễn viên có id = " + id));

        existingActor.setName(request.getName());
        existingActor.setDescription(request.getDescription());
        existingActor.setBirthday(request.getBirthday());
        
        // Chỉ update avatar nếu có giá trị mới, giữ nguyên avatar cũ nếu không có
        if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
            existingActor.setAvatar(request.getAvatar());
        }
        
        return actorRepository.save(existingActor);
    }

    public void deleteActor(Integer id) {
        Actor existingActor = actorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy diễn viên có id = " + id));

        long count = movieRepository.countByActors_Id(id);
        if (count > 0) {
            throw new BadRequestException("Không thể xóa diễn viên này vì đang áp dụng cho phim");
        }

        actorRepository.deleteById(id);
    }
}
