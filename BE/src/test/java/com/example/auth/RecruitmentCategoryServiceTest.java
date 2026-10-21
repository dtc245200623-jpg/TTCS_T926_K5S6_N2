package com.example.auth;

import com.example.auth.dto.RecruitmentCategoryRequest;
import com.example.auth.dto.RecruitmentCategoryResponse;
import com.example.auth.dto.ReorderCategoryItemDto;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.ApplicationRepository;
import com.example.auth.repository.JobRequestRepository;
import com.example.auth.repository.RecruitmentCategoryRepository;
import com.example.auth.service.RecruitmentCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RecruitmentCategoryServiceTest {

    @Autowired
    private RecruitmentCategoryService categoryService;

    @Autowired
    private RecruitmentCategoryRepository categoryRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    private String testSuffix;

    @BeforeEach
    void setUp() {
        testSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Test
    @DisplayName("KN-188: Thêm mới danh mục nguồn ứng viên thành công")
    void testCreateCandidateSource_Success() {
        RecruitmentCategoryRequest req = RecruitmentCategoryRequest.builder()
                .type(CategoryType.CANDIDATE_SOURCE)
                .code("TIKTOK_" + testSuffix)
                .name("TikTok Career Channel " + testSuffix)
                .description("Tuyển dụng qua video ngắn")
                .displayOrder(10)
                .active(true)
                .build();

        RecruitmentCategoryResponse res = categoryService.createCategory(req);

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("TIKTOK_" + testSuffix, res.getCode());
        assertEquals("CANDIDATE_SOURCE", res.getType());
        assertEquals("Nguồn ứng viên", res.getTypeName());
        assertEquals(10, res.getDisplayOrder());
        assertTrue(res.getCanDelete());
    }

    @Test
    @DisplayName("Báo lỗi khi tạo trùng mã trong cùng loại danh mục")
    void testCreateDuplicateCode_ThrowsException() {
        String code = "DUP_" + testSuffix;
        RecruitmentCategoryRequest req1 = RecruitmentCategoryRequest.builder()
                .type(CategoryType.WORK_LOCATION)
                .code(code)
                .name("Địa điểm 1")
                .build();
        categoryService.createCategory(req1);

        RecruitmentCategoryRequest req2 = RecruitmentCategoryRequest.builder()
                .type(CategoryType.WORK_LOCATION)
                .code(code)
                .name("Địa điểm 2")
                .build();

        assertThrows(AppException.class, () -> categoryService.createCategory(req2));
    }

    @Test
    @DisplayName("KN-193: Sắp xếp thứ tự hiển thị cho các danh mục tham chiếu")
    void testReorderCategories_Success() {
        RecruitmentCategory catA = categoryRepository.save(RecruitmentCategory.builder()
                .type(CategoryType.EMPLOYMENT_TYPE)
                .code("REORDER_A_" + testSuffix)
                .name("Loại A")
                .displayOrder(5)
                .active(true)
                .build());

        RecruitmentCategory catB = categoryRepository.save(RecruitmentCategory.builder()
                .type(CategoryType.EMPLOYMENT_TYPE)
                .code("REORDER_B_" + testSuffix)
                .name("Loại B")
                .displayOrder(20)
                .active(true)
                .build());

        // Đảo thứ tự: B lên trước (order=1), A xuống sau (order=2)
        categoryService.reorderCategories(List.of(
                new ReorderCategoryItemDto(catB.getId(), 1),
                new ReorderCategoryItemDto(catA.getId(), 2)
        ));

        List<RecruitmentCategoryResponse> list = categoryService.getCategories(CategoryType.EMPLOYMENT_TYPE, false, "REORDER");
        assertEquals(2, list.size());
        assertEquals("REORDER_B_" + testSuffix, list.get(0).getCode());
        assertEquals(1, list.get(0).getDisplayOrder());
        assertEquals("REORDER_A_" + testSuffix, list.get(1).getCode());
        assertEquals(2, list.get(1).getDisplayOrder());
    }

    @Test
    @DisplayName("KN-192: Chặn xóa giá trị danh mục khi đang được hồ sơ ứng viên tham chiếu")
    void testDeleteCategory_ReferencedByApplication_ThrowsException() {
        // Tạo nguồn ứng viên
        RecruitmentCategory source = categoryRepository.save(RecruitmentCategory.builder()
                .type(CategoryType.CANDIDATE_SOURCE)
                .code("REF_SRC_" + testSuffix)
                .name("Nguồn có tham chiếu")
                .displayOrder(1)
                .active(true)
                .build());

        // Tạo hồ sơ tham chiếu tới nguồn này
        Application app = Application.builder()
                .status("APPLIED")
                .source(source)
                .build();
        applicationRepository.save(app);

        // Kiểm tra canDelete = false
        RecruitmentCategoryResponse detail = categoryService.getCategoryById(source.getId());
        assertEquals(1, detail.getUsageCount());
        assertFalse(detail.getCanDelete());

        // Cố gắng xóa -> Phải ném ngoại lệ chặn xóa
        AppException ex = assertThrows(AppException.class, () -> categoryService.deleteCategory(source.getId()));
        assertTrue(ex.getMessage().contains("Không thể xóa giá trị danh mục"));
        assertTrue(ex.getMessage().contains("đang được tham chiếu"));
    }

    @Test
    @DisplayName("KN-192: Chặn xóa địa điểm khi đang được yêu cầu tuyển dụng tham chiếu")
    void testDeleteCategory_ReferencedByJobRequest_ThrowsException() {
        // Tạo địa điểm làm việc
        RecruitmentCategory location = categoryRepository.save(RecruitmentCategory.builder()
                .type(CategoryType.WORK_LOCATION)
                .code("REF_LOC_" + testSuffix)
                .name("Văn phòng có tham chiếu")
                .displayOrder(1)
                .active(true)
                .build());

        // Tạo yêu cầu tuyển dụng tham chiếu tới địa điểm này
        JobRequest jobReq = JobRequest.builder()
                .title("Tuyển dụng kiểm thử " + testSuffix)
                .location(location)
                .status("OPEN")
                .build();
        jobRequestRepository.save(jobReq);

        // Kiểm tra usageCount = 1
        long usage = categoryService.getUsageCount(location.getId());
        assertEquals(1, usage);

        // Cố gắng xóa -> Phải ném ngoại lệ
        assertThrows(AppException.class, () -> categoryService.deleteCategory(location.getId()));
    }

    @Test
    @DisplayName("KN-192: Cho phép xóa giá trị danh mục khi chưa có bất kỳ dữ liệu nào tham chiếu")
    void testDeleteCategory_NotReferenced_Success() {
        RecruitmentCategory reason = categoryRepository.save(RecruitmentCategory.builder()
                .type(CategoryType.REJECTION_REASON)
                .code("TEMP_REASON_" + testSuffix)
                .name("Lý do tạm thời")
                .displayOrder(99)
                .active(true)
                .build());

        assertDoesNotThrow(() -> categoryService.deleteCategory(reason.getId()));
        assertFalse(categoryRepository.existsById(reason.getId()));
    }
}
