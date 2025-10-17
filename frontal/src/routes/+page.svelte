<script lang="ts">
  import type { PageData } from './$types';
  import type { AuthState } from '$lib/stores/auth';
  import type { FeedQuery } from './+page';
  import type { PostListResponse } from '$lib/api/types';
  import { authStore } from '$lib/stores/auth';
  import PostGrid from '$lib/components/PostGrid.svelte';
  import { getPosts } from '$lib/api/client';
  import { onDestroy, onMount } from 'svelte';

  export let data: PageData;

  let posts: PostListResponse | null = data.initialPosts;
  let query: FeedQuery = { ...data.query };
  let searchTerm = query.q ?? '';
  let loading = false;
  let error: string | null = null;
  let authState: AuthState | null = authStore.snapshot;
  let visibilityFilter: '' | 'public' | 'unlisted' | 'private' = (query.visibility ?? '') as
    | ''
    | 'public'
    | 'unlisted'
    | 'private';

  const unsubscribe = authStore.subscribe((value) => {
    authState = value;
  });

  onDestroy(() => unsubscribe());

  onMount(() => {
    if (!posts && query.owner === 'me' && authState) {
      void loadFeed();
    }
  });

  async function loadFeed(overrides: Partial<FeedQuery> = {}) {
    const shouldResetPage =
      overrides.page === undefined &&
      (overrides.q !== undefined ||
        overrides.owner !== undefined ||
        overrides.visibility !== undefined ||
        overrides.sort !== undefined);

    const nextQuery: FeedQuery = {
      ...query,
      ...overrides,
      page: overrides.page ?? (shouldResetPage ? 1 : query.page),
      per_page: overrides.per_page ?? query.per_page
    };

    if (overrides.q !== undefined && overrides.q.trim() === '') {
      nextQuery.q = undefined;
    }

    if (overrides.visibility === undefined && query.visibility && !nextQuery.visibility) {
      delete nextQuery.visibility;
    }

    if (overrides.owner === undefined && query.owner && !nextQuery.owner) {
      delete nextQuery.owner;
    }

    loading = true;
    error = null;
    posts = null;

    if (nextQuery.owner === 'me' && !authState) {
      query = nextQuery;
      visibilityFilter = (nextQuery.visibility ?? '') as typeof visibilityFilter;
      searchTerm = nextQuery.q ?? '';
      loading = false;
      error = 'Log in to load your private feed.';
      updateUrl(nextQuery);
      return;
    }

    try {
      const response = await getPosts(fetch, nextQuery, nextQuery.owner === 'me' ? authState?.access_token : undefined);
      posts = response;
      query = nextQuery;
      visibilityFilter = (nextQuery.visibility ?? '') as typeof visibilityFilter;
      searchTerm = nextQuery.q ?? '';
      updateUrl(nextQuery);
    } catch (err) {
      console.error(err);
      error = err instanceof Error ? err.message : 'Failed to load posts.';
    } finally {
      loading = false;
    }
  }

  function updateUrl(nextQuery: FeedQuery) {
    if (typeof window === 'undefined') return;
    const params = new URLSearchParams();
    if (nextQuery.page && nextQuery.page !== 1) params.set('page', String(nextQuery.page));
    if (nextQuery.per_page && nextQuery.per_page !== 12) params.set('per_page', String(nextQuery.per_page));
    if (nextQuery.sort && nextQuery.sort !== 'created_desc') params.set('sort', nextQuery.sort);
    if (nextQuery.q) params.set('q', nextQuery.q);
    if (nextQuery.owner) params.set('owner', nextQuery.owner);
    if (nextQuery.visibility) params.set('visibility', nextQuery.visibility);

    const search = params.toString();
    const url = `${window.location.pathname}${search ? `?${search}` : ''}`;
    window.history.replaceState({}, '', url);
  }

  function resetToGlobalFeed() {
    visibilityFilter = '';
    void loadFeed({ owner: undefined, visibility: undefined, page: 1 });
  }

  function toggleMyPosts() {
    if (query.owner === 'me') {
      resetToGlobalFeed();
    } else {
      visibilityFilter = '';
      void loadFeed({ owner: 'me', visibility: undefined, page: 1 });
    }
  }

  function goToPage(direction: 'prev' | 'next') {
    if (!posts) return;
    const totalPages = Math.max(1, Math.ceil(posts.total / posts.per_page));
    if (direction === 'prev' && posts.page > 1) {
      void loadFeed({ page: posts.page - 1 });
    }
    if (direction === 'next' && posts.page < totalPages) {
      void loadFeed({ page: posts.page + 1 });
    }
  }

  function handleSearch(event: SubmitEvent) {
    event.preventDefault();
    void loadFeed({ q: searchTerm });
  }

  function handleVisibilityChange() {
    const value = visibilityFilter === '' ? undefined : visibilityFilter;
    void loadFeed({ visibility: value });
  }

  $: totalPages = posts ? Math.max(1, Math.ceil(posts.total / posts.per_page)) : 1;
</script>

<section class="hero card-surface">
  <h1>Discover the Fonudin feed</h1>
  <p>
    Browse the latest uploads, filter by visibility and search by tags, title or description. Switch to
    <strong>My posts</strong> to manage your own uploads once you are logged in.
  </p>
</section>

<section class="filters card-surface" aria-label="Feed filters">
  <form class="filters-form" on:submit={handleSearch}>
    <label>
      <span>Search</span>
      <input type="text" placeholder="Search posts" bind:value={searchTerm} />
    </label>
    <label>
      <span>Sort</span>
      <select bind:value={query.sort} on:change={() => loadFeed({ sort: query.sort })}>
        <option value="created_desc">Newest</option>
        <option value="created_asc">Oldest</option>
        <option value="popular">Most liked</option>
        <option value="trending">Trending</option>
      </select>
    </label>
    <label>
      <span>Visibility</span>
      <select bind:value={visibilityFilter} on:change={handleVisibilityChange} disabled={query.owner !== 'me'}>
        <option value="">All visibility</option>
        <option value="public">Public only</option>
        <option value="unlisted">Unlisted</option>
        <option value="private">Private</option>
      </select>
    </label>
    <div class="actions">
      <button type="submit" class="search-button">Apply search</button>
      <button type="button" class="secondary" on:click={toggleMyPosts}>
        {query.owner === 'me' ? 'Show global feed' : 'View my posts'}
      </button>
    </div>
  </form>
  {#if error}
    <p class="error" role="alert">{error}</p>
  {/if}
</section>

{#if loading}
  <div class="loading">Loading posts…</div>
{:else if posts}
  <PostGrid posts={posts.items} />
  <div class="pagination">
    <button type="button" on:click={() => goToPage('prev')} disabled={posts.page <= 1}>
      Previous
    </button>
    <span>
      Page {posts.page} of {totalPages}
    </span>
    <button type="button" on:click={() => goToPage('next')} disabled={posts.page >= totalPages}>
      Next
    </button>
  </div>
{:else}
  <div class="empty-results card-surface">
    <p>No posts loaded yet. {query.owner === 'me' ? 'Log in to view your personal feed.' : 'Try updating the filters.'}</p>
  </div>
{/if}

<style>
  .hero {
    padding: clamp(1.5rem, 4vw, 2rem) clamp(1.75rem, 5vw, 2.5rem);
    margin-bottom: clamp(1.5rem, 4vw, 2.5rem);
  }

  .filters {
    margin-bottom: clamp(1.5rem, 5vw, 2.5rem);
    padding: clamp(1.25rem, 4vw, 2rem);
  }

  .filters-form {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 1rem 1.25rem;
  }

  label {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    font-size: 0.9rem;
    color: rgba(226, 232, 240, 0.85);
  }

  label span {
    font-weight: 600;
    letter-spacing: 0.04em;
  }

  .actions {
    display: flex;
    gap: 0.75rem;
    align-items: center;
    justify-content: flex-start;
  }

  .search-button {
    background: linear-gradient(135deg, rgba(56, 189, 248, 0.9), rgba(59, 130, 246, 0.9));
  }

  .secondary {
    background: rgba(148, 163, 184, 0.2);
    border: 1px solid rgba(148, 163, 184, 0.3);
  }

  .error {
    margin-top: 1rem;
    color: #fda4af;
    font-weight: 600;
  }

  .loading {
    padding: 2rem;
    text-align: center;
    color: rgba(226, 232, 240, 0.85);
  }

  .pagination {
    margin-top: 2rem;
    display: flex;
    gap: 1rem;
    align-items: center;
    justify-content: center;
  }

  .pagination span {
    font-weight: 600;
    color: rgba(226, 232, 240, 0.85);
  }

  .empty-results {
    padding: 2rem 1.5rem;
    text-align: center;
    color: rgba(226, 232, 240, 0.8);
  }

  @media (max-width: 720px) {
    .actions {
      flex-direction: column;
      align-items: stretch;
    }
  }
</style>
