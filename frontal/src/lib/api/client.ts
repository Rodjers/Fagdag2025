import { API_BASE_URL } from '$lib/config';
import type {
  AuthTokens,
  Comment,
  CommentListResponse,
  CreateCommentRequest,
  CreatePostRequest,
  ErrorResponse,
  ListCommentsParams,
  ListPostsParams,
  LoginRequest,
  Post,
  PostListResponse,
  UserInfo
} from './types';

type Fetch = typeof fetch;

export class ApiError extends Error {
  readonly status: number;
  readonly data?: ErrorResponse;

  constructor(status: number, message: string, data?: ErrorResponse) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

const jsonHeaders = {
  Accept: 'application/json'
};

function buildUrl(path: string, query?: Record<string, string | number | undefined>) {
  const url = new URL(path, API_BASE_URL);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value === undefined || value === null || value === '') continue;
      url.searchParams.set(key, String(value));
    }
  }
  return url.toString();
}

async function parseJson<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!text) {
    return {} as T;
  }

  try {
    return JSON.parse(text) as T;
  } catch (error) {
    console.error('Failed to parse JSON response', error);
    throw new ApiError(response.status, 'Invalid server response');
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (response.ok) {
    if (response.status === 204) {
      return undefined as T;
    }
    return parseJson<T>(response);
  }

  let errorBody: ErrorResponse | undefined;
  try {
    errorBody = await parseJson<ErrorResponse>(response);
  } catch (error) {
    // ignore parse failure; error handled below
  }

  throw new ApiError(response.status, errorBody?.message ?? response.statusText, errorBody);
}

export async function getPosts(
  fetch: Fetch,
  params: ListPostsParams = {},
  accessToken?: string
): Promise<PostListResponse> {
  const url = buildUrl('/posts', {
    page: params.page,
    per_page: params.per_page,
    sort: params.sort,
    q: params.q,
    owner: params.owner,
    visibility: params.visibility
  });

  const response = await fetch(url, {
    headers: {
      ...jsonHeaders,
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {})
    }
  });

  return handleResponse<PostListResponse>(response);
}

export async function getPost(fetch: Fetch, postId: string, accessToken?: string): Promise<Post> {
  const response = await fetch(buildUrl(`/posts/${postId}`), {
    headers: {
      ...jsonHeaders,
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {})
    }
  });

  return handleResponse<Post>(response);
}

export async function getComments(
  fetch: Fetch,
  postId: string,
  params: ListCommentsParams = {},
  accessToken?: string
): Promise<CommentListResponse> {
  const url = buildUrl(`/posts/${postId}/comments`, {
    page: params.page,
    per_page: params.per_page
  });

  const response = await fetch(url, {
    headers: {
      ...jsonHeaders,
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {})
    }
  });

  return handleResponse<CommentListResponse>(response);
}

export async function createPost(
  fetch: Fetch,
  payload: CreatePostRequest,
  accessToken: string
): Promise<Post> {
  const isFileUpload = typeof File !== 'undefined' && payload.file instanceof File;

  if (isFileUpload) {
    const formData = new FormData();
    formData.append('file', payload.file);

    if (payload.title) {
      formData.append('title', payload.title);
    }

    if (payload.description !== undefined) {
      formData.append('description', payload.description);
    }

    if (payload.tags) {
      for (const tag of payload.tags) {
        formData.append('tags', tag);
      }
    }

    if (payload.visibility) {
      formData.append('visibility', payload.visibility);
    }

    return handleResponse<Post>(
      await fetch(buildUrl('/posts'), {
        method: 'POST',
        headers: {
          ...jsonHeaders,
          Authorization: `Bearer ${accessToken}`
        },
        body: formData
      })
    );
  }

  const url = new URL('/posts', API_BASE_URL);

  if (payload.title) {
    url.searchParams.set('title', payload.title);
  }

  if (payload.description !== undefined) {
    url.searchParams.set('description', payload.description);
  }

  if (payload.tags) {
    for (const tag of payload.tags) {
      url.searchParams.append('tags', tag);
    }
  }

  if (payload.visibility) {
    url.searchParams.set('visibility', payload.visibility);
  }

  if (payload.filename) {
    url.searchParams.set('filename', payload.filename);
  }

  const response = await fetch(url.toString(), {
    method: 'POST',
    headers: {
      ...jsonHeaders,
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/octet-stream'
    },
    body: payload.file
  });

  return handleResponse<Post>(response);
}

export async function createComment(
  fetch: Fetch,
  postId: string,
  payload: CreateCommentRequest,
  accessToken: string
): Promise<Comment> {
  const response = await fetch(buildUrl(`/posts/${postId}/comments`), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...jsonHeaders,
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify(payload)
  });

  return handleResponse<Comment>(response);
}

export async function login(fetch: Fetch, credentials: LoginRequest): Promise<AuthTokens> {
  const response = await fetch(buildUrl('/auth/login'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...jsonHeaders
    },
    body: JSON.stringify(credentials)
  });

  return handleResponse<AuthTokens>(response);
}

export async function refreshToken(fetch: Fetch, refresh_token: string): Promise<AuthTokens> {
  const response = await fetch(buildUrl('/auth/refresh'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...jsonHeaders
    },
    body: JSON.stringify({ refresh_token })
  });

  return handleResponse<AuthTokens>(response);
}

export async function logout(fetch: Fetch, accessToken?: string): Promise<void> {
  const response = await fetch(buildUrl('/auth/logout'), {
    method: 'POST',
    headers: {
      ...jsonHeaders,
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {})
    }
  });

  await handleResponse<void>(response);
}

export async function getUserInfo(_fetch: Fetch, _accessToken: string): Promise<UserInfo> {
  // TODO: replace stubbed response with a real API call when the backend endpoint is ready.
  console.warn('Using stubbed user info until the /auth/userinfo endpoint is available.');

  return new Promise<UserInfo>((resolve) => {
    setTimeout(
      () =>
        resolve({
          id: 'user_demo',
          email: 'demo.user@example.com',
          name: 'Demo User'
        }),
      150
    );
  });
}

export function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date);
}
