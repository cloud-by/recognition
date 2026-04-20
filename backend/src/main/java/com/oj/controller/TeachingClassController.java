package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.OjUser;
import com.oj.entity.Submission;
import com.oj.entity.TeachingClass;
import com.oj.repository.OjUserRepository;
import com.oj.repository.SubmissionRepository;
import com.oj.repository.TeachingClassRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final SubmissionRepository submissionRepository;

    public TeachingClassController(
            TeachingClassRepository classRepository,
            OjUserRepository userRepository,
            SubmissionRepository submissionRepository
    ) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam Long viewerUserId,
            @RequestParam(required = false) String keyword
    ) {
        OjUser viewer = userRepository.findById(viewerUserId).orElse(null);
        if (viewer == null) {
            return ApiResponse.fail("用户不存在");
        }

        String query = keyword == null ? "" : keyword.trim();
        List<TeachingClass> classes;
        if (OjUser.Role.ADMIN.name().equals(viewer.getRole())) {
            classes = query.isBlank()
                    ? classRepository.findAllByOrderByCreatedAtDesc()
                    : classRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(query);
        } else if (OjUser.Role.TEACHER.name().equals(viewer.getRole())) {
            classes = query.isBlank()
                    ? classRepository.findByTeacherIdOrderByCreatedAtDesc(viewerUserId)
                    : classRepository.findByTeacherIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(viewerUserId, query);
        } else {
            return ApiResponse.ok(List.of());
        }

        Set<Long> teacherIds = classes.stream().map(TeachingClass::getTeacherId).collect(Collectors.toSet());
        Map<Long, OjUser> teacherMap = userRepository.findAllById(teacherIds).stream()
                .collect(Collectors.toMap(OjUser::getId, Function.identity()));

        List<ClassListItemResponse> data = classes.stream().map(item -> {
            OjUser teacher = teacherMap.get(item.getTeacherId());
            long studentCount = userRepository.findByTeachingClassIdOrderByIdAsc(item.getId()).stream()
                    .filter(user -> OjUser.Role.STUDENT.name().equals(user.getRole()))
                    .count();
            return new ClassListItemResponse(
                    item.getId(),
                    item.getName(),
                    item.getDescription(),
                    item.getTeacherId(),
                    teacher == null ? "未知老师" : teacher.getNickname(),
                    item.getCreatedAt(),
                    (int) studentCount
            );
        }).toList();
        return ApiResponse.ok(data);
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

    @PostMapping("/{id}/invite")
    public ApiResponse<?> batchInviteStudents(@PathVariable Long id, @RequestBody BatchInviteRequest request) {
        TeachingClass teachingClass = classRepository.findById(id).orElse(null);
        if (teachingClass == null) {
            return ApiResponse.fail("班级不存在");
        }
        OjUser operator = userRepository.findById(request.operatorUserId()).orElse(null);
        if (!canManageClass(operator, teachingClass)) {
            return ApiResponse.fail("无权限操作该班级");
        }

        List<String> normalizedUsernames = request.studentUsernames().stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
        if (normalizedUsernames.isEmpty()) {
            return ApiResponse.fail("请至少填写一个学生用户名");
        }

        List<OjUser> foundUsers = userRepository.findByUsernameIn(normalizedUsernames);
        Map<String, OjUser> foundMap = foundUsers.stream().collect(Collectors.toMap(OjUser::getUsername, Function.identity()));

        List<String> boundStudents = new java.util.ArrayList<>();
        List<String> invalidUsers = new java.util.ArrayList<>();
        for (String username : normalizedUsernames) {
            OjUser student = foundMap.get(username);
            if (student == null || !OjUser.Role.STUDENT.name().equals(student.getRole())) {
                invalidUsers.add(username);
                continue;
            }
            student.setTeachingClassId(teachingClass.getId());
            boundStudents.add(username);
        }
        userRepository.saveAll(foundUsers);

        return ApiResponse.ok(Map.of(
                "classId", id,
                "boundStudents", boundStudents,
                "invalidUsers", invalidUsers,
                "message", "批量邀请处理完成"
        ));
    }

    @GetMapping("/{id}/students/records")
    public ApiResponse<?> classStudentRecords(@PathVariable Long id, @RequestParam Long viewerUserId) {
        TeachingClass teachingClass = classRepository.findById(id).orElse(null);
        if (teachingClass == null) {
            return ApiResponse.fail("班级不存在");
        }
        OjUser viewer = userRepository.findById(viewerUserId).orElse(null);
        if (!canManageClass(viewer, teachingClass)) {
            return ApiResponse.fail("无权限查看该班级");
        }

        List<OjUser> students = userRepository.findByTeachingClassIdOrderByIdAsc(id).stream()
                .filter(item -> OjUser.Role.STUDENT.name().equals(item.getRole()))
                .toList();

        List<StudentRecordResponse> records = students.stream().map(student -> {
            List<Submission> submissions = submissionRepository.findByUserIdOrderBySubmitTimeDesc(student.getId());
            long acCount = submissions.stream().filter(item -> item.getJudgeStatus() == Submission.JudgeStatus.AC).count();
            return new StudentRecordResponse(
                    student.getId(),
                    student.getUsername(),
                    student.getNickname(),
                    submissions.size(),
                    (int) acCount,
                    submissions.isEmpty() ? null : submissions.get(0).getSubmitTime()
            );
        }).toList();

        return ApiResponse.ok(new ClassStudentRecordsResponse(
                teachingClass.getId(),
                teachingClass.getName(),
                records.size(),
                records
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id, @RequestParam Long operatorUserId) {
        TeachingClass teachingClass = classRepository.findById(id).orElse(null);
        if (teachingClass == null) {
            return ApiResponse.fail("班级不存在");
        }
        OjUser operator = userRepository.findById(operatorUserId).orElse(null);
        if (!canManageClass(operator, teachingClass)) {
            return ApiResponse.fail("仅创建该班级的老师或管理员可删除");
        }
        List<OjUser> students = userRepository.findByTeachingClassIdOrderByIdAsc(id);
        students.forEach(item -> item.setTeachingClassId(null));
        userRepository.saveAll(students);

        classRepository.delete(teachingClass);
        return ApiResponse.ok("删除成功");
    }

    private boolean canManageClass(OjUser operator, TeachingClass teachingClass) {
        if (operator == null) {
            return false;
        }
        if (OjUser.Role.ADMIN.name().equals(operator.getRole())) {
            return true;
        }
        return OjUser.Role.TEACHER.name().equals(operator.getRole())
                && teachingClass.getTeacherId().equals(operator.getId());
    }

    public record CreateClassRequest(@NotNull Long teacherId, @NotBlank String name, String description) {}

    public record BatchInviteRequest(@NotNull Long operatorUserId, @NotEmpty List<String> studentUsernames) {}

    public record ClassListItemResponse(
            Long id,
            String name,
            String description,
            Long teacherId,
            String teacherNickname,
            LocalDateTime createdAt,
            Integer studentCount
    ) {}

    public record StudentRecordResponse(
            Long userId,
            String username,
            String nickname,
            Integer totalSubmissions,
            Integer acCount,
            LocalDateTime lastSubmitTime
    ) {}

    public record ClassStudentRecordsResponse(
            Long classId,
            String className,
            Integer studentCount,
            List<StudentRecordResponse> students
    ) {}
}