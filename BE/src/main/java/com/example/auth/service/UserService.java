package com.example.auth.service;

import com.example.auth.dto.UserRequest;
import com.example.auth.dto.UserResponse;

import java.util.List;
import java.util.Set;

/**
 * Interface UserService định nghĩa các thao tác nghiệp vụ liên quan đến quản lý người dùng (User).
 */
public interface UserService {
    
    /**
     * Lấy danh sách tất cả người dùng, hỗ trợ tìm kiếm, lọc theo vai trò (role), trạng thái khóa (isLocked) và phân trang.
     * 
     * @param search Từ khóa tìm kiếm (ví dụ: username, họ tên...)
     * @param role Tên vai trò dùng để lọc danh sách (ví dụ: ROLE_ADMIN, ROLE_USER...)
     * @param isLocked Lọc theo trạng thái tài khoản (true: bị khóa, false: đang hoạt động)
     * @param pageable Đối tượng chứa thông tin phân trang (trang số mấy, số lượng phần tử trên 1 trang)
     * @return Trả về một đối tượng Page chứa danh sách người dùng (dưới dạng UserResponse)
     */
    org.springframework.data.domain.Page<UserResponse> getAllUsers(String search, String role, Boolean isLocked, org.springframework.data.domain.Pageable pageable);
    
    /**
     * Tạo mới một người dùng vào hệ thống.
     * 
     * @param request Đối tượng chứa các thông tin đăng ký của người dùng mới (username, password, v.v.)
     * @return Trả về thông tin người dùng vừa được tạo thành công (dưới dạng UserResponse)
     */
    UserResponse createUser(UserRequest request);
    
    /**
     * Cập nhật danh sách các vai trò (phân quyền) cho một người dùng.
     * 
     * @param id Mã định danh (ID) của người dùng cần thay đổi quyền
     * @param roles Tập hợp các quyền mới sẽ được gán cho người dùng này
     * @return Trả về thông tin người dùng sau khi đã cập nhật quyền thành công
     */
    UserResponse updateUserRoles(Long id, Set<String> roles);
    
    /**
     * Chuyển đổi trạng thái khóa của người dùng (Đang hoạt động -> Bị khóa, hoặc Bị khóa -> Đang hoạt động).
     * 
     * @param id Mã định danh (ID) của người dùng cần khóa/mở khóa
     * @return Trả về thông tin người dùng sau khi đã thay đổi trạng thái khóa
     */
    UserResponse toggleLockUser(Long id);
    
    /**
     * Dành cho Quản trị viên (Admin) thay đổi mật khẩu của một người dùng bất kỳ.
     * 
     * @param id Mã định danh (ID) của người dùng bị đổi mật khẩu
     * @param newPassword Mật khẩu mới được thiết lập
     * @return Trả về thông tin người dùng sau khi đã đổi mật khẩu thành công
     */
    UserResponse adminChangePassword(Long id, String newPassword);
    
    /**
     * Xóa hoàn toàn một người dùng khỏi hệ thống.
     * 
     * @param id Mã định danh (ID) của người dùng cần xóa
     */
    void deleteUser(Long id);

    /**
     * Tạo file mẫu Excel hoặc CSV để quản trị viên tải về điền dữ liệu.
     * 
     * @param format "xlsx" hoặc "csv"
     * @return Dữ liệu nhị phân của file mẫu
     */
    byte[] generateImportTemplate(String format);

    /**
     * Đọc và kiểm tra dữ liệu từng dòng trước khi nhập vào hệ thống.
     * 
     * @param file File Excel hoặc CSV chứa danh sách người dùng
     * @return Danh sách các dòng kèm trạng thái hợp lệ và lỗi chi tiết
     */
    List<com.example.auth.dto.UserImportRowDto> previewImportUsers(org.springframework.web.multipart.MultipartFile file);

    /**
     * Nhập người dùng từ file: bỏ qua các dòng lỗi, lưu các dòng hợp lệ vào CSDL.
     * 
     * @param file File Excel hoặc CSV chứa danh sách người dùng
     * @return Báo cáo tổng kết quá trình nhập
     */
    com.example.auth.dto.ImportSummaryResponse importUsers(org.springframework.web.multipart.MultipartFile file);

    /**
     * Lấy thông tin hồ sơ cá nhân của người dùng hiện tại đang đăng nhập.
     * 
     * @param username Tên đăng nhập của người dùng
     * @return Thông tin hồ sơ người dùng
     */
    UserResponse getProfile(String username);

    /**
     * Cập nhật thông tin hồ sơ cá nhân (chỉ cho phép sửa họ tên, số điện thoại, chức danh).
     * Tuyệt đối không thay đổi email, phòng ban, vai trò, tên đăng nhập.
     * Kiểm tra định dạng số điện thoại Việt Nam hợp lệ.
     * 
     * @param username Tên đăng nhập của người dùng cần cập nhật
     * @param request Dữ liệu cập nhật hồ sơ cá nhân
     * @return Thông tin hồ sơ sau khi cập nhật
     */
    UserResponse updateProfile(String username, com.example.auth.dto.UserProfileRequest request);

    /**
     * Cập nhật ảnh đại diện người dùng:
     * - Chấp nhận JPG/PNG tối đa 2MB (KN-200)
     * - Ảnh được cắt vuông và tạo bản thu nhỏ (KN-201)
     * 
     * @param username Tên đăng nhập của người dùng
     * @param file Tệp ảnh tải lên
     * @return Thông tin hồ sơ sau khi cập nhật ảnh đại diện
     */
    UserResponse updateAvatar(String username, org.springframework.web.multipart.MultipartFile file);
}


