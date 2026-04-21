# Docker Deployment

This project is deployed as separate frontend and backend containers.

- `frontend`: builds the Vite app and serves static files with Nginx.
- `backend`: builds and runs the Spring Boot jar.
- `mysql`: stores application data.
- `redis`: provides Redis for the backend.

The browser accesses the frontend only. Nginx proxies `/api/*` requests to the backend container.

## Server Steps

1. Install Docker and Docker Compose on the server.

2. Copy the project to the server.

3. Create the runtime environment file:

```bash
cp .env.example .env
```

Edit `.env` and replace all passwords and `JUDGE0_BASE_URL`.
If Judge0 runs on the same server outside this compose file, keep `JUDGE0_BASE_URL=http://host.docker.internal:2358`.

4. Start the services:

```bash
docker compose up -d --build
```

5. View logs:

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

6. Open:

```text
http://your-server-ip/
```

## Important Notes

- The backend keeps problem test data under `/app/files` in the container.
- The compose file mounts `./backend/files` to `/app/files`, so put test data under `backend/files/problem/<problemId>/`.
- The frontend build uses `VITE_API_BASE_URL=/api`, so API calls stay on the same domain and go through Nginx.
- MySQL initialization from `sql/mysql.sql` only runs when the `mysql-data` volume is first created.

## Common Commands

Rebuild and restart:

```bash
docker compose up -d --build
```

Stop:

```bash
docker compose down
```

Stop and remove database data:

```bash
docker compose down -v
```

Use `down -v` carefully because it deletes MySQL and Redis volumes.
