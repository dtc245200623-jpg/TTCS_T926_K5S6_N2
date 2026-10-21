package com.example.auth.security;

import com.example.auth.entity.User;
import com.example.auth.repository.TokenBlacklistRepository;
import com.example.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Kiểm tra blacklist
                if (tokenBlacklistRepository.findByToken(jwt).isPresent()) {
                    log.warn("Token đã bị thu hồi trong blacklist!");
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = tokenProvider.getUsernameFromToken(jwt);
                Long tokenVersionInJwt = tokenProvider.getTokenVersionFromToken(jwt);

                User user = userRepository.findByUsername(username).orElse(null);

                if (user != null) {
                    /*
                     * KIỂM TRA CƠ CHẾ TOKEN_VERSION:
                     * Nếu token_version trong JWT khác với token_version hiện tại trong DB,
                     * chứng tỏ người dùng đã đổi mật khẩu hoặc đăng xuất toàn bộ thiết bị.
                     * Token này đã bị thu hồi/vô hiệu hóa!
                     */
                    if (tokenVersionInJwt == null || !tokenVersionInJwt.equals(user.getTokenVersion())) {
                        log.warn("Phiên đăng nhập đã bị vô hiệu hóa cho user [{}]. JWT version: {}, DB version: {}",
                                username, tokenVersionInJwt, user.getTokenVersion());
                        // Không xác thực, SecurityContext sẽ rỗng -> Spring Security chặn 401 Unauthorized
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Token hợp lệ và version trùng khớp -> Cấp quyền cho SecurityContext
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            log.error("Không thể thiết lập xác thực người dùng trong Security Context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
