# BE
26-1 졸업프로젝트 Recipiti BackEnd 레포지토리

## Nginx 영수증 이미지 업로드 설정

배포 서버의 Nginx 기본 요청 본문 제한(약 1MiB)으로 인해 React Native에서
영수증 이미지를 업로드할 때 `413 Request Entity Too Large`가 발생할 수 있습니다.

저장소의 Nginx 설정을 배포 서버에 적용합니다.

```bash
sudo cp deploy/nginx/receipiti-upload.conf /etc/nginx/conf.d/receipiti-upload.conf
sudo nginx -t
sudo systemctl reload nginx
sudo nginx -T | grep -n client_max_body_size
```

현재 저장소의 `docker-compose.yml`에는 Nginx 서비스가 없으므로, 배포 서버에서
Nginx를 직접 실행한다면 위 명령을 사용합니다.

Nginx를 Docker Compose로 운영하는 경우에는 해당 Compose 파일의 `nginx`
서비스에 다음 읽기 전용 마운트를 추가합니다.

```yaml
services:
  nginx:
    volumes:
      - ./deploy/nginx/receipiti-upload.conf:/etc/nginx/conf.d/receipiti-upload.conf:ro
```

설정을 추가한 뒤 Nginx 컨테이너를 재생성하고, 컨테이너 내부에 설정이 실제로
반영됐는지 확인합니다.

```bash
docker compose up -d --force-recreate nginx
docker compose exec nginx nginx -t
docker compose exec nginx nginx -T | grep -n client_max_body_size
```

출력에 `client_max_body_size 25m;`가 표시되어야 합니다. 실제 Compose 서비스명이
`nginx`가 아니라면 위 명령의 `nginx`를 해당 서비스명으로 변경합니다.

현재 Nginx 요청 제한은 Spring의
`spring.servlet.multipart.max-request-size=25MB`와 동일한 25MB입니다.
