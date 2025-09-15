package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Actor;

public interface ActorRepository extends JpaRepository<Actor, Integer> {
}