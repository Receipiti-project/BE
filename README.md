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
```

Nginx를 Docker로 실행하는 경우에는
`deploy/nginx/receipiti-upload.conf`를 컨테이너의
`/etc/nginx/conf.d/receipiti-upload.conf`에 마운트한 뒤 컨테이너를 재시작합니다.

현재 Nginx 요청 제한은 Spring의
`spring.servlet.multipart.max-request-size=25MB`와 동일한 25MB입니다.
