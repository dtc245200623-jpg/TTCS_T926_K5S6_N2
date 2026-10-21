package com.example.auth.service.impl;

import com.example.auth.dto.DepartmentManagerDto;
import com.example.auth.dto.DepartmentRequest;
import com.example.auth.dto.DepartmentResponse;
import com.example.auth.entity.Department;
import com.example.auth.entity.User;
import com.example.auth.exception.AppException;
import com.example.auth.repository.DepartmentRepository;
import com.example.auth.repository.JobRequestRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final JobRequestRepository jobRequestRepository;

    private static final List<String> OPEN_JOB_STATUSES = List.of("OPEN", "PENDING_APPROVAL", "APPROVED");

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentTree(Boolean activeOnly) {
        List<Department> allDepts = departmentRepository.findAllWithDetails();

        if (Boolean.TRUE.equals(activeOnly)) {
            allDepts = allDepts.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getActive()))
                    .collect(Collectors.toList());
        }

        Map<Long, DepartmentResponse> responseMap = new LinkedHashMap<>();
        for (Department dept : allDepts) {
            responseMap.put(dept.getId(), toResponse(dept));
        }

        List<DepartmentResponse> rootList = new ArrayList<>();
        for (Department dept : allDepts) {
            DepartmentResponse currentResp = responseMap.get(dept.getId());
            if (dept.getParent() != null && responseMap.containsKey(dept.getParent().getId())) {
                DepartmentResponse parentResp = responseMap.get(dept.getParent().getId());
                parentResp.getChildren().add(currentResp);
                parentResp.setHasChildren(true);
            } else {
                rootList.add(currentResp);
            }
        }

        // Tính toán level cấp bậc cho các node trong cây
        for (DepartmentResponse root : rootList) {
            calculateLevels(root, 1);
        }

        return rootList;
    }

    private void calculateLevels(DepartmentResponse node, int currentLevel) {
        node.setLevel(currentLevel);
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            node.setHasChildren(true);
            for (DepartmentResponse child : node.getChildren()) {
                calculateLevels(child, currentLevel + 1);
            }
        } else {
            node.setHasChildren(false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments(Boolean activeOnly, String search) {
        List<Department> allDepts = departmentRepository.findAllWithDetails();

        return allDepts.stream()
                .filter(d -> {
                    if (Boolean.TRUE.equals(activeOnly) && !Boolean.TRUE.equals(d.getActive())) {
                        return false;
                    }
                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.trim().toLowerCase();
                        boolean matchName = d.getName() != null && d.getName().toLowerCase().contains(s);
                        boolean matchCode = d.getCode() != null && d.getCode().toLowerCase().contains(s);
                        boolean matchManager = d.getManager() != null && (
                                (d.getManager().getFullName() != null && d.getManager().getFullName().toLowerCase().contains(s)) ||
                                (d.getManager().getUsername() != null && d.getManager().getUsername().toLowerCase().contains(s))
                        );
                        return matchName || matchCode || matchManager;
                    }
                    return true;
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng ban với mã ID: " + id));
        return toResponse(dept);
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        validateDepartmentRequest(request, null);

        Department parent = null;
        if (request.getParentId() != null) {
            parent = departmentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException("Không tìm thấy phòng ban cha với mã ID: " + request.getParentId()));
        }

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new AppException("Không tìm thấy nhân viên phụ trách với ID: " + request.getManagerId()));
        }

        Department department = Department.builder()
                .name(request.getName().trim())
                .code(request.getCode() != null ? request.getCode().trim().toUpperCase() : null)
                .description(request.getDescription())
                .parent(parent)
                .manager(manager)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Đã tạo mới phòng ban: {} (ID: {})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng ban với mã ID: " + id));

        validateDepartmentRequest(request, id);

        // Kiểm tra logic phòng ban cha (KN-174: Chống chu trình khép kín / circular reference)
        Department parent = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new AppException("Một phòng ban không thể tự chọn chính mình làm phòng ban cha.");
            }
            if (isDescendant(request.getParentId(), id)) {
                throw new AppException("Không thể chọn phòng ban trực thuộc (cấp con/cháu) làm phòng ban cha.");
            }
            parent = departmentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException("Không tìm thấy phòng ban cha với mã ID: " + request.getParentId()));
        }

        // Kiểm tra người phụ trách (KN-175)
        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new AppException("Không tìm thấy nhân viên phụ trách với ID: " + request.getManagerId()));
        }

        department.setName(request.getName().trim());
        department.setCode(request.getCode() != null ? request.getCode().trim().toUpperCase() : null);
        department.setDescription(request.getDescription());
        department.setParent(parent);
        department.setManager(manager);
        if (request.getActive() != null) {
            department.setActive(request.getActive());
        }

        Department updated = departmentRepository.save(department);
        log.info("Đã cập nhật phòng ban: {} (ID: {})", updated.getName(), updated.getId());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public DepartmentResponse assignManager(Long departmentId, Long managerId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng ban với mã ID: " + departmentId));

        User manager = null;
        if (managerId != null) {
            manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new AppException("Không tìm thấy nhân viên với ID: " + managerId));
        }

        department.setManager(manager);
        Department saved = departmentRepository.save(department);
        log.info("Đã gán người phụ trách [{}] cho phòng ban: {}", 
                manager != null ? manager.getFullName() : "Trống", saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentManagerDto> getEligibleManagers() {
        return userRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsLocked()))
                .map(u -> DepartmentManagerDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName() != null ? u.getFullName() : u.getUsername())
                        .email(u.getEmail())
                        .title(u.getTitle())
                        .department(u.getDepartment())
                        .build())
                .sorted(Comparator.comparing(DepartmentManagerDto::getFullName))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DepartmentResponse toggleStatus(Long id, Boolean active) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng ban với mã ID: " + id));

        boolean newActive = active != null ? active : !Boolean.TRUE.equals(department.getActive());
        department.setActive(newActive);
        Department saved = departmentRepository.save(department);
        log.info("Đã chuyển trạng thái phòng ban {} sang: {}", saved.getName(), newActive ? "Đang áp dụng" : "Ngừng áp dụng");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng ban với mã ID: " + id));

        // Quy tắc 1: Không thể xóa nếu đang có các phòng ban con trực thuộc (KN-174)
        if (departmentRepository.existsByParentId(id)) {
            throw new AppException("Không thể xóa phòng ban đang có các phòng ban trực thuộc cấp con. Vui lòng di chuyển hoặc xóa các phòng ban con trước.");
        }

        // Quy tắc 2: Không xoá phòng ban đang có yêu cầu tuyển dụng mở (KN-176)
        boolean hasOpenRequests = jobRequestRepository.existsByDepartmentIdAndStatusIn(id, OPEN_JOB_STATUSES);
        if (hasOpenRequests) {
            long openCount = jobRequestRepository.countByDepartmentIdAndStatusIn(id, OPEN_JOB_STATUSES);
            throw new AppException(String.format(
                    "Không thể xóa phòng ban '%s' do đang có %d yêu cầu tuyển dụng mở / đang xử lý. Vui lòng hoàn thành/đóng các yêu cầu hoặc chuyển sang trạng thái ngừng áp dụng.",
                    department.getName(), openCount
            ));
        }

        // Quy tắc 3: Nếu phòng ban đã có lịch sử yêu cầu tuyển dụng trong quá khứ -> Khuyến nghị và chặn xóa cứng để bảo vệ dữ liệu
        long totalRequests = jobRequestRepository.countByDepartmentId(id);
        if (totalRequests > 0) {
            throw new AppException(String.format(
                    "Phòng ban '%s' đã có dữ liệu lịch sử (%d yêu cầu tuyển dụng). Để bảo toàn dữ liệu hệ thống, vui lòng chuyển sang trạng thái ngừng áp dụng thay vì xóa.",
                    department.getName(), totalRequests
            ));
        }

        departmentRepository.delete(department);
        log.info("Đã xóa hoàn toàn phòng ban: {} (ID: {})", department.getName(), id);
    }

    /**
     * Kiểm tra candidateParentId có phải là hậu duệ (con/cháu) của currentId không
     */
    private boolean isDescendant(Long candidateParentId, Long currentId) {
        Long nextParentId = candidateParentId;
        Set<Long> visited = new HashSet<>();
        while (nextParentId != null && visited.add(nextParentId)) {
            if (nextParentId.equals(currentId)) {
                return true;
            }
            Department p = departmentRepository.findById(nextParentId).orElse(null);
            if (p != null && p.getParent() != null) {
                nextParentId = p.getParent().getId();
            } else {
                break;
            }
        }
        return false;
    }

    private void validateDepartmentRequest(DepartmentRequest request, Long currentId) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException("Tên phòng ban không được để trống.");
        }

        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            String code = request.getCode().trim().toUpperCase();
            boolean exists = currentId == null
                    ? departmentRepository.existsByCodeIgnoreCase(code)
                    : departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
            if (exists) {
                throw new AppException("Mã phòng ban '" + code + "' đã tồn tại trên hệ thống.");
            }
        }
    }

    private DepartmentResponse toResponse(Department dept) {
        long totalJobs = jobRequestRepository.countByDepartmentId(dept.getId());
        long openJobs = jobRequestRepository.countByDepartmentIdAndStatusIn(dept.getId(), OPEN_JOB_STATUSES);
        boolean hasChildren = departmentRepository.existsByParentId(dept.getId());

        Department parent = dept.getParent();
        User manager = dept.getManager();

        return DepartmentResponse.builder()
                .id(dept.getId())
                .code(dept.getCode())
                .name(dept.getName())
                .description(dept.getDescription())
                .parentId(parent != null ? parent.getId() : null)
                .parentName(parent != null ? parent.getName() : null)
                .parentCode(parent != null ? parent.getCode() : null)
                .hasChildren(hasChildren)
                .children(new ArrayList<>())
                .managerId(manager != null ? manager.getId() : null)
                .managerName(manager != null ? (manager.getFullName() != null ? manager.getFullName() : manager.getUsername()) : null)
                .managerEmail(manager != null ? manager.getEmail() : null)
                .managerUsername(manager != null ? manager.getUsername() : null)
                .managerTitle(manager != null ? manager.getTitle() : null)
                .active(dept.getActive() != null ? dept.getActive() : true)
                .totalJobRequests(totalJobs)
                .openJobRequests(openJobs)
                .createdAt(dept.getCreatedAt())
                .updatedAt(dept.getUpdatedAt())
                .build();
    }
}
