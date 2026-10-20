# Hướng dẫn đẩy dự án lên GitLab

Dự án hiện có **hai Git repository riêng**: frontend trong `fe/` và backend ở thư mục gốc. Cần push từng repository; push ở thư mục gốc không đẩy frontend.

## 1. Kiểm tra trước khi push

Mở PowerShell và kiểm tra repository, branch, remote cùng các file sẽ được đưa lên:

```powershell
Set-Location "D:\Năm 3\RaiWay99\QuanLiSanBong\fe"
git rev-parse --show-toplevel
git status --short
git remote -v
git branch --show-current
```

Chỉ stage những file thuộc thay đổi muốn gửi. Tránh `git add .` khi còn file chưa rà soát.

```powershell
git add src\App.jsx src\components\Layout.jsx
# Thay danh sách trên bằng đúng các file cần commit.
git diff --cached --check
git diff --cached --stat
git diff --cached
```

Trước khi commit, kiểm tra diff đã stage không chứa `.env`, mật khẩu, token, private key hoặc thông tin kết nối DB. Repo này có cấu hình ứng dụng chứa thông tin nhạy cảm; hãy chuyển secret sang biến môi trường/GitLab CI/CD Variables và xoay vòng secret nếu chúng đã được push hoặc chia sẻ.

## 2. Push frontend

```powershell
Set-Location "D:\Năm 3\RaiWay99\QuanLiSanBong\fe"
npm ci
npm run build
git status --short
```

Stage đúng các file FE, kiểm tra diff như mục 1, sau đó commit và push branch hiện tại:

```powershell
git add <cac-file-frontend-can-gui>
git diff --cached --check
git diff --cached --stat
git commit -m "feat: add staff booking pages"
$branch = git branch --show-current
git push -u origin $branch
```

Trong PowerShell, thay `<cac-file-frontend-can-gui>` bằng đường dẫn file thật; không nhập nguyên dấu `< >`.

## 3. Push backend

```powershell
Set-Location "D:\Năm 3\RaiWay99\QuanLiSanBong"
git rev-parse --show-toplevel
git status --short
git remote -v
git branch --show-current
```

Build/test backend trước khi stage nếu Maven có trong `PATH`:

```powershell
mvn test
```

Sau đó chỉ stage các file BE cần gửi, kiểm tra diff và push:

```powershell
git add <cac-file-backend-can-gui>
git diff --cached --check
git diff --cached --stat
git diff --cached
git commit -m "feat: add staff booking APIs"
$branch = git branch --show-current
git push -u origin $branch
```

Thay `<cac-file-backend-can-gui>` bằng đường dẫn file thật. Không stage thư mục `fe/` trong repository BE; frontend có repository riêng.

## 4. Kiểm tra remote

Mỗi repository cần có remote tên `origin`. Nếu `git remote -v` không hiển thị remote, tạo project trên GitLab rồi thêm URL HTTPS/SSH của project đó:

```powershell
git remote add origin https://gitlab.com/GROUP/PROJECT.git
git remote -v
```

Thay `GROUP/PROJECT` bằng namespace và tên project thật. Nếu `origin` đã tồn tại, không chạy lại `git remote add`; kiểm tra URL trước khi push.

Nếu GitLab từ chối push vì branch remote đã có commit, cập nhật bằng rebase rồi push lại:

```powershell
git pull --rebase origin <ten-branch>
git push -u origin <ten-branch>
```

Thay `<ten-branch>` bằng branch đang làm. Không dùng `git push --force` để xử lý lỗi này nếu chưa thống nhất với nhóm.

## 5. Theo dõi pipeline

Sau khi push, mở **Build > Pipelines** trong GitLab và xem log job bị lỗi. Pipeline có thể cần GitLab Runner được đăng ký đúng tag của FE/BE. Các job package/deploy của cấu hình hiện tại chỉ chạy trên branch mặc định.

Với frontend, đặt `VITE_API_BASE_URL` trong **Settings > CI/CD > Variables** trước khi build/deploy; Vite nhúng giá trị này vào bundle lúc build. Không đặt token hoặc mật khẩu trực tiếp trong file CI hay lệnh Git.
