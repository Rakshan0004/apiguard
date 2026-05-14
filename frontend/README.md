# APIGuard Frontend (TanStack Start)

## Features
- Separate `/login` and `/dashboard` pages
- Register and login
- Register APIs
- Create plans per API
- Generate API keys for a selected API + plan
- View your APIs and generated keys

## Run
```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

By default the app calls `http://localhost:8081` (`VITE_API_BASE_URL`).
