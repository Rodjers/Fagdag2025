export interface ErrorResponse {
  error: string;
  message: string;
  request_id?: string;
}

export interface AuthTokens {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

export interface PostSummary {
  id: string;
  title: string;
  owner_id: string;
  created_at: string;
  updated_at?: string;
  thumbnail_url?: string;
  visibility: 'public' | 'unlisted' | 'private';
  tags?: string[];
  like_count?: number;
}

export interface Post extends PostSummary {
  description?: string;
  file_id?: string;
  file_url?: string;
  width?: number;
  height?: number;
  content_type?: string;
  size_bytes?: number;
  comment_count?: number;
}

export interface PaginatedResponse<T> {
  items: T[];
  page: number;
  per_page: number;
  total: number;
}

export type PostListResponse = PaginatedResponse<PostSummary>;
export type CommentListResponse = PaginatedResponse<Comment>;

export interface PostMetadataPatch {
  title?: string;
  description?: string;
  tags?: string[];
  visibility?: 'public' | 'unlisted' | 'private';
}

export interface Comment {
  id: string;
  post_id: string;
  author_id: string;
  text: string;
  created_at: string;
}

export interface ListPostsParams {
  page?: number;
  per_page?: number;
  sort?: 'created_desc' | 'created_asc' | 'popular' | 'trending';
  q?: string;
  owner?: string;
  visibility?: 'public' | 'unlisted' | 'private';
}

export interface ListCommentsParams {
  page?: number;
  per_page?: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface CreateCommentRequest {
  text: string;
}
