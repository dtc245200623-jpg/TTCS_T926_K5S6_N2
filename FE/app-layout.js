// =========================================================================
// HỆ THỐNG TUYỂN DỤNG NỘI BỘ - ĐỒNG BỘ GIAO DIỆN CHUNG (APP-LAYOUT.JS)
// Hiển thị danh mục chức năng theo Chức vụ / Nghiệp vụ (Accordions)
// Tự động mở nhóm chức năng tương ứng và đồng bộ 100% trên mọi trang
// =========================================================================

const APP_BRAND_NAME = "Tuyển dụng Nội bộ";

const APP_ROLE_GROUPS = [
    {
        roleCode: "ROLE_CANDIDATE",
        name: "Ứng viên",
        icon: `<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>`,
        tasks: [
            { title: "Trang việc làm công khai", link: "career-portal.html" },
            { title: "Danh sách Việc làm", link: "dashboard.html?task=Danh sách Việc làm&role=ROLE_CANDIDATE", taskName: "Danh sách Việc làm" },
            { title: "Nộp CV", link: "dashboard.html?task=Nộp CV&role=ROLE_CANDIDATE", taskName: "Nộp CV" },
            { title: "Theo dõi Hồ sơ", link: "dashboard.html?task=Theo dõi Hồ sơ&role=ROLE_CANDIDATE", taskName: "Theo dõi Hồ sơ" },
            { title: "Lịch phỏng vấn", link: "dashboard.html?task=Lịch phỏng vấn&role=ROLE_CANDIDATE", taskName: "Lịch phỏng vấn" },
            { title: "Phản hồi Offer", link: "dashboard.html?task=Phản hồi Offer&role=ROLE_CANDIDATE", taskName: "Phản hồi Offer" }
        ]
    },
    {
        roleCode: "ROLE_RECRUITER",
        name: "Nhân viên tuyển dụng",
        icon: `<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>`,
        tasks: [
            { title: "Đăng tin tuyển dụng", link: "jobs.html?tab=APPROVED" },
            { title: "Sàng lọc CV", link: "dashboard.html?task=Sàng lọc CV&role=ROLE_RECRUITER", taskName: "Sàng lọc CV" },
            { title: "Điều phối Pipeline", link: "dashboard.html?task=Điều phối Pipeline&role=ROLE_RECRUITER", taskName: "Điều phối Pipeline" },
            { title: "Đặt lịch phỏng vấn", link: "dashboard.html?task=Đặt lịch phỏng vấn&role=ROLE_RECRUITER", taskName: "Đặt lịch phỏng vấn" },
            { title: "Soạn Offer", link: "dashboard.html?task=Soạn Offer&role=ROLE_RECRUITER", taskName: "Soạn Offer" },
            { title: "Cổng việc làm", link: "career-portal.html" },
            { title: "Cổng ứng tuyển ngoài", link: "careers.html" }
        ]
    },
    {
        roleCode: "ROLE_HIRING_MANAGER",
        name: "Trưởng bộ phận",
        icon: `<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/></svg>`,
        tasks: [
            { title: "Yêu cầu Tuyển dụng", link: "jobs.html" },
            { title: "Tạo nhanh Yêu cầu", link: "dashboard.html?task=Tạo nhanh Yêu cầu&role=ROLE_HIRING_MANAGER", taskName: "Tạo nhanh Yêu cầu" },
            { title: "Xem ứng viên bộ phận", link: "dashboard.html?task=Xem ứng viên bộ phận&role=ROLE_HIRING_MANAGER", taskName: "Xem ứng viên bộ phận" },
            { title: "Quyết định tuyển", link: "dashboard.html?task=Quyết định tuyển&role=ROLE_HIRING_MANAGER", taskName: "Quyết định tuyển" }
        ]
    },
    {
        roleCode: "ROLE_INTERVIEWER",
        name: "Người phỏng vấn",
        icon: `<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>`,
        tasks: [
            { title: "Xem lịch phỏng vấn", link: "dashboard.html?task=Xem lịch phỏng vấn&role=ROLE_INTERVIEWER", taskName: "Xem lịch phỏng vấn" },
            { title: "Đọc CV ứng viên", link: "dashboard.html?task=Đọc CV ứng viên&role=ROLE_INTERVIEWER", taskName: "Đọc CV ứng viên" },
            { title: "Ngân hàng câu hỏi", link: "questions.html" },
            { title: "Phiếu Đánh giá", link: "competencies.html" }
        ]
    },
    {
        roleCode: "ROLE_HR_MANAGER",
        name: "Trưởng phòng Nhân sự",
        icon: `<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>`,
        tasks: [
            { title: "Cơ cấu Phòng ban", link: "departments.html" },
            { title: "Chức danh & Dải lương", link: "positions.html" },
            { title: "Khung năng lực & Đánh giá", link: "competencies.html" },
            { title: "Ngân hàng câu hỏi", link: "questions.html" },
            { title: "Danh mục tham chiếu", link: "categories.html" },
            { title: "Yêu cầu Tuyển dụng", link: "jobs.html" },
            { title: "Chuỗi cấp duyệt", link: "approval-workflows.html" },
            { title: "Kế hoạch Headcount", link: "headcount-plans.html" },
            { title: "Trang việc làm", link: "career-portal.html" },
            { title: "Giám sát Vị trí", link: "dashboard.html?task=Giám sát Vị trí&role=ROLE_HR_MANAGER", taskName: "Giám sát Vị trí" },
            { title: "Theo dõi Ngân sách", link: "dashboard.html?task=Theo dõi Ngân sách&role=ROLE_HR_MANAGER", taskName: "Theo dõi Ngân sách" },
            { title: "Báo cáo Tuyển dụng", link: "dashboard.html?task=Báo cáo&role=ROLE_HR_MANAGER", taskName: "Báo cáo" }
        ]
    },
    {
        roleCode: "ROLE_APPROVER",
        name: "Người duyệt",
        icon: `<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>`,
        tasks: [
            { title: "Duyệt Yêu cầu Tuyển dụng", link: "jobs.html" },
            { title: "Duyệt Offer vượt hạn mức", link: "dashboard.html?task=Duyệt Offer vượt hạn mức&role=ROLE_APPROVER", taskName: "Duyệt Offer vượt hạn mức" }
        ]
    },
    {
        roleCode: "ROLE_ADMIN",
        name: "Quản trị hệ thống",
        icon: `<svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>`,
        tasks: [
            { title: "Quản lý Tài khoản", link: "users.html" },
            { title: "Nhật ký hệ thống", link: "dashboard.html?task=Nhật ký hệ thống&role=ROLE_ADMIN", taskName: "Nhật ký hệ thống" }
        ]
    }
];

function ensureSidebarHeader() {
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) return;
    let header = sidebar.querySelector('.sidebar-header');
    if (!header) {
        header = document.createElement('div');
        header.className = 'sidebar-header';
        sidebar.insertBefore(header, sidebar.firstChild);
    }
    header.innerHTML = `
        <svg width="24" height="24" fill="none" stroke="#2563eb" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path>
        </svg>
        <h2>${APP_BRAND_NAME}</h2>
    `;
}

function toggleAppMenuGroup(headerEl) {
    const groupEl = headerEl.closest('.menu-group');
    if (!groupEl) return;
    const contentEl = groupEl.querySelector('.menu-group-content');
    if (!contentEl) return;
    
    const roleCode = groupEl.getAttribute('data-role');
    const isCurrentlyOpen = contentEl.style.display !== 'none';
    
    if (isCurrentlyOpen) {
        contentEl.style.display = 'none';
        headerEl.classList.remove('expanded');
        if (roleCode) {
            sessionStorage.setItem('role_collapsed_' + roleCode, 'true');
            sessionStorage.removeItem('role_expanded_' + roleCode);
        }
    } else {
        contentEl.style.display = 'block';
        headerEl.classList.add('expanded');
        if (roleCode) {
            sessionStorage.setItem('role_expanded_' + roleCode, 'true');
            sessionStorage.removeItem('role_collapsed_' + roleCode);
        }
    }
}

function handleAppTaskClick(event, taskTitle, roleCode, link) {
    const currentPath = window.location.pathname.toLowerCase();
    const isDashboard = currentPath.endsWith('dashboard.html') || currentPath.endsWith('/') || currentPath.endsWith('index.html');

    if (link && link.includes('task=')) {
        if (isDashboard && typeof window.loadTaskView === 'function') {
            if (event && event.preventDefault) event.preventDefault();
            // Highlight active item
            document.querySelectorAll('.menu-item, .menu-sub-item').forEach(el => el.classList.remove('active'));
            if (event && event.currentTarget) event.currentTarget.classList.add('active');
            window.loadTaskView(event, taskTitle, roleCode);
            return;
        }
        return;
    }

    if (!link || link === '#') {
        if (event && event.preventDefault) event.preventDefault();
    }
}

function renderAppSidebar(userRoles = []) {
    ensureSidebarHeader();
    const menuContainer = document.getElementById('sidebarMenu');
    if (!menuContainer) return;

    if (!userRoles || userRoles.length === 0) {
        try {
            userRoles = JSON.parse(localStorage.getItem('userRoles') || '[]');
        } catch (e) {
            userRoles = [];
        }
    }

    const currentPath = window.location.pathname.toLowerCase();
    const isDashboard = currentPath.endsWith('dashboard.html') || currentPath.endsWith('/') || currentPath.endsWith('index.html');

    const urlParams = new URLSearchParams(window.location.search);
    const activeTaskFromUrl = urlParams.get('task');

    let html = '';

    // 1. Dashboard Top Link
    const isDashboardActive = isDashboard && !activeTaskFromUrl;
    html += `
        <a class="menu-item ${isDashboardActive ? 'active' : ''}" href="dashboard.html">
            <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></svg>
            <span>Tổng quan Dashboard</span>
        </a>
        <div class="menu-section-label">Chức năng theo Chức vụ</div>
    `;

    // 2. Role Groups (Accordions)
    APP_ROLE_GROUPS.forEach(group => {
        // User has access if role is assigned or user is admin
        const hasAccess = userRoles.includes(group.roleCode) || userRoles.includes('ROLE_ADMIN');
        if (!hasAccess) return;

        let groupHasActiveTask = false;
        let taskItemsHtml = '';

        group.tasks.forEach(task => {
            const taskLink = task.link.toLowerCase();
            let isItemActive = false;

            if (task.taskName) {
                if (isDashboard && activeTaskFromUrl === task.taskName) {
                    isItemActive = true;
                }
            } else {
                if (currentPath.endsWith(taskLink) || currentPath.endsWith('/' + taskLink)) {
                    isItemActive = true;
                }
            }

            if (isItemActive) {
                groupHasActiveTask = true;
            }

            taskItemsHtml += `
                <a class="menu-sub-item ${isItemActive ? 'active' : ''}" href="${task.link}" onclick="handleAppTaskClick(event, '${task.taskName || task.title}', '${group.roleCode}', '${task.link}')">
                    <span class="sub-bullet"></span>
                    <span>${task.title}</span>
                </a>
            `;
        });

        // Determine if group is expanded:
        // Expanded if current page is inside this group, or if user opened it previously
        const isExpandedBySession = sessionStorage.getItem('role_expanded_' + group.roleCode) === 'true';
        const isCollapsedBySession = sessionStorage.getItem('role_collapsed_' + group.roleCode) === 'true';
        let isExpanded = groupHasActiveTask;
        if (!isExpanded && isExpandedBySession && !isCollapsedBySession) {
            isExpanded = true;
        }

        html += `
            <div class="menu-group" data-role="${group.roleCode}">
                <div class="menu-group-header ${isExpanded ? 'expanded' : ''}" onclick="toggleAppMenuGroup(this)">
                    <div class="header-left">
                        ${group.icon}
                        <span>${group.name}</span>
                    </div>
                    <svg class="chevron" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7"></path></svg>
                </div>
                <div class="menu-group-content" style="display: ${isExpanded ? 'block' : 'none'};">
                    ${taskItemsHtml}
                </div>
            </div>
        `;
    });

    // 3. Divider & Account / Profile Links
    const isProfileActive = currentPath.endsWith('profile.html');
    const isPasswordActive = currentPath.endsWith('change-password.html');
    html += `
        <div class="menu-divider"></div>
        <div class="menu-section-label">Tài khoản & Cá nhân</div>
        <a class="menu-item ${isProfileActive ? 'active' : ''}" href="profile.html">
            <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
            <span>Hồ sơ cá nhân</span>
        </a>
        <a class="menu-item ${isPasswordActive ? 'active' : ''}" href="change-password.html">
            <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4v-3.252l7.5-7.5A6 6 0 0115 7z"/></svg>
            <span>Đổi mật khẩu</span>
        </a>
    `;

    menuContainer.innerHTML = html;
}

function ensureAppStyles() {
    if (!document.querySelector('link[href*="app-layout.css"]')) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = 'app-layout.css?v=2';
        document.head.appendChild(link);
    }
}

// Global binds
window.APP_BRAND_NAME = APP_BRAND_NAME;
window.APP_ROLE_GROUPS = APP_ROLE_GROUPS;
window.renderAppSidebar = renderAppSidebar;
window.renderSidebar = renderAppSidebar;
window.toggleAppMenuGroup = toggleAppMenuGroup;
window.handleAppTaskClick = handleAppTaskClick;

document.addEventListener('DOMContentLoaded', () => {
    ensureAppStyles();
    ensureSidebarHeader();
    renderAppSidebar();

    // Check if on dashboard and loaded with task param
    const currentPath = window.location.pathname.toLowerCase();
    const isDashboard = currentPath.endsWith('dashboard.html') || currentPath.endsWith('/') || currentPath.endsWith('index.html');
    if (isDashboard) {
        const urlParams = new URLSearchParams(window.location.search);
        const taskParam = urlParams.get('task');
        const roleParam = urlParams.get('role');
        if (taskParam && typeof window.loadTaskView === 'function') {
            setTimeout(() => {
                window.loadTaskView({ preventDefault: () => {} }, taskParam, roleParam || 'ROLE_ADMIN');
            }, 120);
        }
    }
});
