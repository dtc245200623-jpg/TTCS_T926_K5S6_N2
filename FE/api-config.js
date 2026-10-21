// Cấu hình URL mặc định cho Backend
// Khi dùng chung mạng LAN, đổi 'localhost' thành IP của máy chạy Backend (VD: '192.168.1.55')
// Khi deploy lên mạng, đổi thành link server thật.
const API_BASE_URL = 'http://localhost:8080';

// Khối lệnh này tự động điều chỉnh URL cho tất cả các request fetch trong ứng dụng.
// Đảm bảo không phải sửa code thủ công ở từng file HTML.
const originalFetch = window.fetch;
window.fetch = async function(...args) {
    let resource = args[0];
    
    if (typeof resource === 'string') {
        if (resource.startsWith('/api')) {
            args[0] = API_BASE_URL + resource;
        } else if (resource.startsWith('http://localhost:8080')) {
            args[0] = resource.replace('http://localhost:8080', API_BASE_URL);
        }
    }
    
    return originalFetch.apply(this, args);
};
