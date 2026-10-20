# Giải thích pipeline CI/CD (`.gitlab-ci.yml`)

Tài liệu này mô tả mục đích và ý nghĩa của từng dòng trong file `.gitlab-ci.yml`
của project **be-cicd** (Spring Boot 2.7.0 / Java 11).

## Tổng quan luồng chạy

Pipeline chạy tuần tự qua 4 giai đoạn (stage):

```
build  ->  test  ->  package  ->  deploy
(compile)  (unit test)  (docker image)  (chạy container)
```

Mỗi lần bạn `git push`, GitLab tự động kích hoạt pipeline. Một stage chỉ chạy
khi stage trước đó thành công.

---

## 1. Khai báo các stage

```yaml
stages:
  - build
  - test
  - package
  - deploy
```

- `stages`: định nghĩa thứ tự các giai đoạn của pipeline.
- GitLab chạy các job theo đúng thứ tự khai báo ở đây. Các job cùng một stage
  chạy song song; các stage khác nhau chạy tuần tự.

---

## 2. Biến toàn cục (`variables`)

```yaml
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository ..."
  MAVEN_CLI_OPTS: "-B --no-transfer-progress"
  IMAGE_TAG: "$CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA"
  IMAGE_LATEST: "$CI_REGISTRY_IMAGE:latest"
```

- `MAVEN_OPTS`: đặt thư mục Maven local repository (`.m2/repository`) nằm trong
  project. Nhờ vậy các thư viện tải về có thể được **cache** và tái sử dụng giữa
  các lần chạy, giúp pipeline nhanh hơn. Phần `-Dorg.slf4j...` tắt bớt log tải
  dependency cho gọn.
- `MAVEN_CLI_OPTS`: tham số dùng chung cho mọi lệnh `mvn`.
  - `-B` (batch mode): chạy không tương tác, phù hợp môi trường CI.
  - `--no-transfer-progress`: ẩn thanh tiến trình tải file cho log sạch.
- `IMAGE_TAG`: tên image Docker gắn theo mã commit ngắn
  (`$CI_COMMIT_SHORT_SHA`) — mỗi commit có một image riêng, dễ truy vết.
- `IMAGE_LATest`: tag `latest` luôn trỏ tới bản mới nhất.

> Các biến bắt đầu bằng `$CI_...` là **biến dựng sẵn** do GitLab tự cung cấp,
> bạn không cần khai báo. Ví dụ: `$CI_REGISTRY_IMAGE` là đường dẫn image trong
> Container Registry của project, `$CI_PROJECT_DIR` là thư mục gốc project trên runner.

---

## 3. Cache Maven

```yaml
cache:
  key: "$CI_COMMIT_REF_SLUG"
  paths:
    - .m2/repository
```

- `cache`: lưu lại thư mục để dùng cho lần chạy sau, tránh tải lại từ đầu.
- `key: "$CI_COMMIT_REF_SLUG"`: cache được tách riêng theo từng nhánh (branch),
  nên nhánh này không ảnh hưởng cache của nhánh khác.
- `paths: .m2/repository`: chính là thư mục chứa các dependency đã tải về.

---

## 4. Stage `build` — biên dịch

```yaml
build:
  stage: build
  image: maven:3.8.6-eclipse-temurin-11
  script:
    - mvn $MAVEN_CLI_OPTS clean compile
  artifacts:
    paths:
      - target/
    expire_in: 1 hour
```

- `stage: build`: gán job này vào giai đoạn `build`.
- `image: maven:3.8.6-eclipse-temurin-11`: dùng image Docker có sẵn Maven và
  **JDK 11** — đúng phiên bản Java mà project yêu cầu.
- `script`: các lệnh chạy trong job.
  - `mvn clean compile`: xóa build cũ rồi biên dịch mã nguồn.
- `artifacts`: file/thư mục cần giữ lại và chuyển sang stage sau.
  - `paths: target/`: giữ thư mục build.
  - `expire_in: 1 hour`: tự xóa artifact sau 1 giờ để tiết kiệm dung lượng.

---

## 5. Stage `test` — chạy unit test

```yaml
test:
  stage: test
  image: maven:3.8.6-eclipse-temurin-11
  script:
    - mvn $MAVEN_CLI_OPTS test
  artifacts:
    when: always
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
    paths:
      - target/surefire-reports/
    expire_in: 1 week
```

- `mvn test`: chạy toàn bộ unit test.
- `artifacts.when: always`: giữ lại kết quả **kể cả khi test fail** (để còn xem
  báo cáo lỗi).
- `reports.junit`: chỉ cho GitLab file kết quả test dạng JUnit XML. GitLab sẽ
  hiển thị số test pass/fail trực tiếp trong giao diện Merge Request và pipeline.
- `expire_in: 1 week`: giữ báo cáo 1 tuần.

> Hiện project chưa có test nào nên stage này sẽ báo "No tests to run" và vẫn
> pass. Khi bạn thêm test, kết quả sẽ tự hiện lên UI.

---

## 6. Stage `package` — đóng gói và đẩy Docker image

```yaml
package:
  stage: package
  image: docker:24.0
  services:
    - docker:24.0-dind
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  before_script:
    - echo "$CI_REGISTRY_PASSWORD" | docker login -u "$CI_REGISTRY_USER" --password-stdin "$CI_REGISTRY"
  script:
    - docker build -t "$IMAGE_TAG" -t "$IMAGE_LATEST" .
    - docker push "$IMAGE_TAG"
    - docker push "$IMAGE_LATEST"
  rules:
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
```

- `image: docker:24.0`: dùng Docker CLI để build image.
- `services: docker:24.0-dind`: bật **Docker-in-Docker** — cho phép chạy lệnh
  `docker build` bên trong job CI.
- `DOCKER_TLS_CERTDIR: "/certs"`: bật kết nối bảo mật (TLS) giữa Docker client và
  daemon dind.
- `before_script`: chạy trước `script`.
  - Đăng nhập vào GitLab Container Registry bằng credentials dựng sẵn.
    `--password-stdin` đưa mật khẩu qua đường ống thay vì ghi thẳng ra lệnh (an toàn hơn).
- `script`:
  - `docker build ... .`: build image từ `Dockerfile` ở thư mục gốc, gắn 2 tag.
  - `docker push`: đẩy image lên registry.
- `rules`: điều kiện để job chạy.
  - `$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH`: chỉ build/push image khi commit
    nằm trên **nhánh mặc định** (thường là `main`/`master`). Nhánh feature sẽ
    không tạo image, tránh rác registry.

---

## 7. Stage `deploy` — triển khai (mẫu)

```yaml
deploy:
  stage: deploy
  image: docker:24.0
  services:
    - docker:24.0-dind
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  before_script:
    - echo "$CI_REGISTRY_PASSWORD" | docker login -u "$CI_REGISTRY_USER" --password-stdin "$CI_REGISTRY"
  script:
    - docker pull "$IMAGE_TAG"
    - docker rm -f be-cicd || true
    - docker run -d --name be-cicd -p 8084:8084 "$IMAGE_TAG"
  environment:
    name: staging
    url: http://localhost:8084
  rules:
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
      when: manual
```

- `script`:
  - `docker pull`: tải image vừa build ở stage trước.
  - `docker rm -f be-cicd || true`: xóa container cũ nếu có. `|| true` để lệnh
    không fail khi container chưa tồn tại.
  - `docker run -d ... -p 8084:8084`: chạy container mới ở chế độ nền, map cổng 8084.
- `environment`: khai báo môi trường triển khai (ví dụ `staging`), giúp GitLab
  theo dõi lịch sử deploy trong mục **Environments**.
- `rules ... when: manual`: job **không tự chạy** mà chờ bạn bấm nút deploy thủ
  công trên GitLab — tránh triển khai ngoài ý muốn.

> Đây chỉ là bản mẫu chạy container ngay trên runner. Thực tế bạn sẽ thay bằng:
> SSH tới server rồi chạy `docker compose up`, hoặc `kubectl apply` với Kubernetes,
> tùy môi trường đích.

---

## 8. Biến GitLab dựng sẵn đã dùng

| Biến | Ý nghĩa |
|------|---------|
| `$CI_PROJECT_DIR` | Thư mục gốc project trên runner |
| `$CI_COMMIT_SHORT_SHA` | Mã commit ngắn (8 ký tự) |
| `$CI_COMMIT_REF_SLUG` | Tên nhánh đã "làm sạch" để dùng an toàn |
| `$CI_COMMIT_BRANCH` | Tên nhánh của commit hiện tại |
| `$CI_DEFAULT_BRANCH` | Nhánh mặc định của repo (main/master) |
| `$CI_REGISTRY` | Địa chỉ Container Registry |
| `$CI_REGISTRY_IMAGE` | Đường dẫn image của project trong registry |
| `$CI_REGISTRY_USER` / `$CI_REGISTRY_PASSWORD` | Thông tin đăng nhập registry (tự cấp) |

---

## 9. Điều kiện tiên quyết để pipeline chạy được

1. Project đã bật **Container Registry** trong GitLab (mặc định thường có sẵn).
2. GitLab Runner hỗ trợ Docker và cho phép chạy dịch vụ `dind` (privileged mode).
3. File `Dockerfile` nằm ở thư mục gốc — đã có sẵn trong project này.
