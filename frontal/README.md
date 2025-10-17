# OnlyPikks frontal

SvelteKit-based frontend scaffold for the Posts Service API. It consumes the public REST endpoints for feed
browsing, authentication, post details and comments.

## Getting started

```bash
npm install
npm run dev
```

The frontend expects the backend to be available at `https://api.example.com` by default. Override this with a
`PUBLIC_API_BASE_URL` environment variable at build or runtime:

```bash
PUBLIC_API_BASE_URL=https://localhost:8080 npm run dev
```

## Features

- Global feed with search, sorting, pagination and an authenticated "My posts" toggle
- Post detail page with media preview, metadata and dynamic comments
- Authentication flow wired to `/auth/login`, persisted in local storage with logout support
- API client helpers and typed models generated from the provided OpenAPI schema
- Reusable UI components for post cards, grids and comment lists styled with a glassmorphism aesthetic

## Next steps

- Implement post creation, editing and deletion using the multipart endpoints
- Add token refresh and user profile discovery using `/auth/refresh` and future profile endpoints
- Introduce routing guards and server hooks to inject JWTs automatically in server-side loads
- Expand testing coverage with Playwright component tests and contract tests against the API
