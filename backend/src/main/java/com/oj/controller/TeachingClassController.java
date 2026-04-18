package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.OjUser;
import com.oj.entity.TeachingClass;
import com.oj.repository.OjUserRepository;
import com.oj.repository.TeachingClassRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/classes")
public class TeachingClassController {

    private final TeachingClassRepository classRepository;
    private final OjUserRepository userRepository;

    public TeachingClassController(TeachingClassRepository classRepository, OjUserRepository userRepository) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<TeachingClass>> list(@RequestParam Long teacherId) {
        return ApiResponse.ok(classRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody CreateClassRequest request) {
        OjUser teacher = userRepository.findById(request.teacherId()).orElse(null);
        if (teacher == null || !OjUser.Role.TEACHER.name().equals(teacher.getRole())) {
            return ApiResponse.fail("仅老师可创建班级");
        }
        TeachingClass teachingClass = new TeachingClass();
        teachingClass.setTeacherId(request.teacherId());
        teachingClass.setName(request.name().trim());
        teachingClass.setDescription(request.description());
        return ApiResponse.ok(classRepository.save(teachingClass));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id, @RequestParam Long teacherId) {
        TeachingClass teachingClass = classRepository.findById(id).orElse(null);
        if (teachingClass == null) {
            return ApiResponse.fail("班级不存在");
        }
        if (!teachingClass.getTeacherId().equals(teacherId)) {
            return ApiResponse.fail("仅创建该班级的老师可删除");
        }
        classRepository.delete(teachingClass);
        return ApiResponse.ok("删除成功");
    }

    public record CreateClassRequest(@NotNull Long teacherId, @NotBlank String name, String description) {}
}