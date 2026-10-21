package com.example.auth;

import com.example.auth.dto.DepartmentRequest;
import com.example.auth.dto.DepartmentResponse;
import com.example.auth.entity.Department;
import com.example.auth.entity.JobRequest;
import com.example.auth.entity.User;
import com.example.auth.exception.AppException;
import com.example.auth.repository.DepartmentRepository;
import com.example.auth.repository.JobRequestRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DepartmentServiceTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    private User managerUser;

    @BeforeEach
    void setUp() {
        managerUser = userRepository.findByUsername("test_dept_manager").orElse(null);
        if (managerUser == null) {
            managerUser = User.builder()
                    .username("test_dept_manager")
                    .email("manager_dept_test@company.com")
                    .fullName("Nguyễn Văn Trưởng Phòng")
                    .password("123456@")
                    .roles(Set.of("ROLE_HIRING_MANAGER"))
                    .build();
            managerUser = userRepository.save(managerUser);
        }
    }

    private String randomCode(String prefix) {
        return (prefix + "_" + UUID.randomUUID().toString().substring(0, 6)).toUpperCase();
    }

    @Test
    @DisplayName("KN-174: Thiết kế và triển khai cấu trúc cây phòng ban nhiều cấp (Tree Hierarchy)")
    void testMultiLevelDepartmentTree() {
        String rootCode = randomCode("TECH");
        String child1Code = randomCode("DEV");
        String child2Code = randomCode("BE");

        // 1. Tạo Root Department: Khối Kỹ Thuật
        DepartmentRequest rootReq = DepartmentRequest.builder()
                .code(rootCode)
                .name("Khối Kỹ Thuật " + rootCode)
                .description("Phụ trách toàn bộ mảng kỹ thuật")
                .active(true)
                .build();
        DepartmentResponse rootResp = departmentService.createDepartment(rootReq);
        assertNotNull(rootResp.getId());

        // 2. Tạo Cấp 2: Trung tâm Phát triển phần mềm (trực thuộc TECH)
        DepartmentRequest child1Req = DepartmentRequest.builder()
                .code(child1Code)
                .name("Trung tâm Phát triển Phần mềm " + child1Code)
                .parentId(rootResp.getId())
                .active(true)
                .build();
        DepartmentResponse child1Resp = departmentService.createDepartment(child1Req);
        assertEquals(rootResp.getId(), child1Resp.getParentId());

        // 3. Tạo Cấp 3: Nhóm Backend (trực thuộc DEV_CENTER)
        DepartmentRequest child2Req = DepartmentRequest.builder()
                .code(child2Code)
                .name("Nhóm Kỹ sư Backend " + child2Code)
                .parentId(child1Resp.getId())
                .active(true)
                .build();
        DepartmentResponse child2Resp = departmentService.createDepartment(child2Req);
        assertEquals(child1Resp.getId(), child2Resp.getParentId());

        // 4. Lấy cây phân cấp
        List<DepartmentResponse> tree = departmentService.getDepartmentTree(false);
        assertFalse(tree.isEmpty());

        DepartmentResponse rootInTree = tree.stream()
                .filter(d -> d.getId().equals(rootResp.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(rootInTree);
        assertEquals(1, rootInTree.getLevel());
        assertTrue(rootInTree.getHasChildren());
        assertEquals(1, rootInTree.getChildren().size());

        DepartmentResponse childInTree = rootInTree.getChildren().get(0);
        assertEquals(child1Code, childInTree.getCode());
        assertEquals(2, childInTree.getLevel());
        assertTrue(childInTree.getHasChildren());
        assertEquals(1, childInTree.getChildren().size());

        DepartmentResponse grandChildInTree = childInTree.getChildren().get(0);
        assertEquals(child2Code, grandChildInTree.getCode());
        assertEquals(3, grandChildInTree.getLevel());
        assertFalse(grandChildInTree.getHasChildren());
    }

    @Test
    @DisplayName("KN-174: Chống chu trình khép kín / vòng lặp trong phân cấp phòng ban")
    void testPreventCircularHierarchy() {
        String c1 = randomCode("D1");
        String c2 = randomCode("D2");

        DepartmentRequest dept1Req = DepartmentRequest.builder().code(c1).name("Phòng 1").build();
        DepartmentResponse dept1 = departmentService.createDepartment(dept1Req);

        DepartmentRequest dept2Req = DepartmentRequest.builder().code(c2).name("Phòng 2").parentId(dept1.getId()).build();
        DepartmentResponse dept2 = departmentService.createDepartment(dept2Req);

        // Thử gán chính nó làm cha của chính nó
        DepartmentRequest selfParentReq = DepartmentRequest.builder().code(c1).name("Phòng 1").parentId(dept1.getId()).build();
        assertThrows(AppException.class, () -> departmentService.updateDepartment(dept1.getId(), selfParentReq));

        // Thử gán con (dept2) làm cha của cha (dept1) -> Vòng lặp
        DepartmentRequest cycleReq = DepartmentRequest.builder().code(c1).name("Phòng 1").parentId(dept2.getId()).build();
        assertThrows(AppException.class, () -> departmentService.updateDepartment(dept1.getId(), cycleReq));
    }

    @Test
    @DisplayName("KN-175: Bổ sung chức năng gán người phụ trách cho từng phòng ban")
    void testAssignDepartmentManager() {
        String c = randomCode("HR");
        DepartmentRequest req = DepartmentRequest.builder()
                .code(c)
                .name("Phòng Nhân sự " + c)
                .managerId(managerUser.getId())
                .build();
        DepartmentResponse resp = departmentService.createDepartment(req);

        assertEquals(managerUser.getId(), resp.getManagerId());
        assertEquals(managerUser.getFullName(), resp.getManagerName());
        assertEquals(managerUser.getEmail(), resp.getManagerEmail());

        // Kiểm tra API gán lại người phụ trách (assignManager)
        DepartmentResponse reassigned = departmentService.assignManager(resp.getId(), null);
        assertNull(reassigned.getManagerId());

        DepartmentResponse reassignedBack = departmentService.assignManager(resp.getId(), managerUser.getId());
        assertEquals(managerUser.getId(), reassignedBack.getManagerId());
    }

    @Test
    @DisplayName("KN-176: Triển khai quy tắc không xoá phòng ban đang có yêu cầu tuyển dụng mở, chỉ ngừng áp dụng")
    void testCannotDeleteDepartmentWithOpenJobRequests() {
        String c = randomCode("SALES");
        DepartmentRequest req = DepartmentRequest.builder()
                .code(c)
                .name("Phòng Kinh doanh " + c)
                .active(true)
                .build();
        DepartmentResponse dept = departmentService.createDepartment(req);

        // Tạo JobRequest ở trạng thái OPEN liên kết tới phòng ban này
        Department deptEntity = departmentRepository.findById(dept.getId()).orElseThrow();
        JobRequest openJob = JobRequest.builder()
                .title("Tuyển Chuyên viên Kinh doanh B2B")
                .department(deptEntity)
                .status("OPEN")
                .headcount(3)
                .build();
        jobRequestRepository.save(openJob);

        // Cố gắng xoá phòng ban -> Phải bị chặn và ném AppException
        AppException ex = assertThrows(AppException.class, () -> departmentService.deleteDepartment(dept.getId()));
        assertTrue(ex.getMessage().contains("yêu cầu tuyển dụng mở"));

        // Chuyển sang trạng thái ngừng áp dụng (toggleStatus) -> Phải thành công
        DepartmentResponse deactivated = departmentService.toggleStatus(dept.getId(), false);
        assertFalse(deactivated.getActive());

        // Kích hoạt lại áp dụng -> Thành công
        DepartmentResponse reactivated = departmentService.toggleStatus(dept.getId(), true);
        assertTrue(reactivated.getActive());
    }

    @Test
    @DisplayName("KN-176: Không xoá phòng ban đang có phòng ban cấp con")
    void testCannotDeleteDepartmentWithChildren() {
        String pCode = randomCode("P1");
        String cCode = randomCode("C1");

        DepartmentRequest parentReq = DepartmentRequest.builder().code(pCode).name("Phòng Cha").build();
        DepartmentResponse parent = departmentService.createDepartment(parentReq);

        DepartmentRequest childReq = DepartmentRequest.builder().code(cCode).name("Phòng Con").parentId(parent.getId()).build();
        DepartmentResponse child = departmentService.createDepartment(childReq);

        // Thử xoá phòng cha -> Phải ném ngoại lệ
        AppException ex = assertThrows(AppException.class, () -> departmentService.deleteDepartment(parent.getId()));
        assertTrue(ex.getMessage().contains("phòng ban trực thuộc"));

        // Xoá phòng con trước -> Thành công
        departmentService.deleteDepartment(child.getId());

        // Giờ xoá phòng cha -> Thành công
        assertDoesNotThrow(() -> departmentService.deleteDepartment(parent.getId()));
    }
}
