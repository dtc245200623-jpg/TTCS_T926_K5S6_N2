package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system-categories")
@RequiredArgsConstructor
public class SystemCategoryController {
    
    private final DepartmentRepository departmentRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();
        
        // Departments
        long deptCount = departmentRepository.count();
        Map<String, Object> depts = new HashMap<>();
        depts.put("name", "Phòng ban");
        depts.put("count", deptCount + " phòng ban");
        depts.put("lastUpdated", "Hôm nay");
        categories.add(depts);
        
        // Skills (Mock)
        Map<String, Object> skills = new HashMap<>();
        skills.put("name", "Kỹ năng (Skills)");
        skills.put("count", "12 kỹ năng");
        skills.put("lastUpdated", "1 tuần trước");
        categories.add(skills);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục thành công", categories));
    }
}
