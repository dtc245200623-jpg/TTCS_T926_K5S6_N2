package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerPortalConfigRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 255, message = "Tên công ty không quá 255 ký tự")
    private String companyName;

    @Size(max = 255, message = "Khẩu hiệu không quá 255 ký tự")
    private String tagline;

    @NotBlank(message = "Nội dung giới thiệu công ty không được để trống")
    private String introduction;

    private String aboutCulture;

    private String benefits;

    private String logoUrl;

    private String bannerUrl;

    @Size(max = 255, message = "Email không quá 255 ký tự")
    private String email;

    @Size(max = 50, message = "Số điện thoại không quá 50 ký tự")
    private String phone;

    @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
    private String address;

    @Size(max = 255, message = "Website không quá 255 ký tự")
    private String website;

    @Size(max = 255, message = "Giờ làm việc không quá 255 ký tự")
    private String workingHours;

    private Boolean isPublished;
}
