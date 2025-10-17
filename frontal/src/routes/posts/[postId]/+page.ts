import type { PageLoad } from './$types';
import { getComments, getPost } from '$lib/api/client';
import type { CommentListResponse, Post } from '$lib/api/types';

export interface PostPageData {
  post: Post;
  comments: CommentListResponse;
}

export const load: PageLoad = async ({ fetch, params }) => {
  const [post, comments] = await Promise.all([
    getPost(fetch, params.postId),
    getComments(fetch, params.postId, { page: 1, per_page: 20 })
  ]);

  return { post, comments } satisfies PostPageData;
};
