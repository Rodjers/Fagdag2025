import { API_BASE_URL } from '$lib/config';
import type {
  AuthTokens,
  Comment,
  CommentListResponse,
  CreateCommentRequest,
  ErrorResponse,
  ListCommentsParams,
  ListPostsParams,
  LoginRequest,
  Post,
  PostListResponse
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
