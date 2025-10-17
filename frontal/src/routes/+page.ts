import type { PageLoad } from './$types';
import { getPosts } from '$lib/api/client';
import type { ListPostsParams, PostListResponse } from '$lib/api/types';

export interface FeedQuery extends ListPostsParams {
  page: number;
  per_page: number;
}

export const load: PageLoad = async ({ fetch, url }) => {
  const page = Number(url.searchParams.get('page') ?? '1');
  const per_page = Number(url.searchParams.get('per_page') ?? '12');
  const sort = (url.searchParams.get('sort') as FeedQuery['sort']) ?? 'created_desc';
  const q = url.searchParams.get('q') ?? undefined;
  const owner = url.searchParams.get('owner') ?? undefined;
  const visibility = (url.searchParams.get('visibility') as FeedQuery['visibility']) ?? undefined;

  const query: FeedQuery = {
    page: Number.isFinite(page) && page > 0 ? page : 1,
    per_page: Number.isFinite(per_page) && per_page > 0 ? per_page : 12,
    sort,
    q,
    owner,
    visibility
  };

  let initialPosts: PostListResponse | null = null;
  if (owner !== 'me') {
    initialPosts = await getPosts(fetch, query);
  }

  return {
    initialPosts,
    query
  } satisfies { initialPosts: PostListResponse | null; query: FeedQuery };
};
