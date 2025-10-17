<script lang="ts">
  import type { PageData } from './$types';
  import { CommentList } from '$lib';
  import { createComment, formatDate, getComments, ApiError } from '$lib/api/client';
  import type { Comment } from '$lib/api/types';
  import { authStore, type AuthState } from '$lib/stores/auth';
  import { onDestroy } from 'svelte';
  import { API_BASE_URL } from '$lib/config';

  export let data: PageData;

  const post = data.post;
  const mediaUrl =
    post.file_url ?? post.thumbnail_url ?? (post.file_id ? `${API_BASE_URL}/files/${post.file_id}` : null);
  const perPage = data.comments.per_page;
  let comments: Comment[] = [...data.comments.items];
  let currentPage = data.comments.page;
  let commentTotal = post.comment_count ?? data.comments.total;
  let submitting = false;
  let loadingMore = false;
  let error: string | null = null;
  let commentsError: string | null = null;
  let commentText = '';
  let authState: AuthState | null = authStore.snapshot;

  const unsubscribe = authStore.subscribe((value) => {
    authState = value;
  });

  onDestroy(() => unsubscribe());

  async function handleComment(event: SubmitEvent) {
    event.preventDefault();
    if (!authState) {
      error = 'You need to log in before posting a comment.';
      return;
    }

    const text = commentText.trim();
    if (!text) {
      error = 'Please enter a comment.';
      return;
    }

    submitting = true;
    error = null;

    try {
      const comment = await createComment(fetch, post.id, { text }, authState.access_token);
      comments = [comment, ...comments];
      commentText = '';
      commentTotal += 1;
    } catch (err) {
      console.error(err);
      if (err instanceof ApiError) {
        error = err.data?.message ?? 'Failed to publish comment.';
      } else if (err instanceof Error) {
        error = err.message;
      } else {
        error = 'Failed to publish comment.';
      }
    } finally {
      submitting = false;
    }
  }

  async function loadMoreComments() {
    if (loadingMore) return;
    const totalPages = Math.max(1, Math.ceil(commentTotal / perPage));
    if (currentPage >= totalPages) return;

    loadingMore = true;
    commentsError = null;

    try {
      const response = await getComments(
        fetch,
        post.id,
        { page: currentPage + 1, per_page: perPage },
        authState?.access_token
      );
      comments = [...comments, ...response.items];
      currentPage = response.page;
      commentTotal = response.total;
    } catch (err) {
      console.error(err);
      commentsError = err instanceof Error ? err.message : 'Unable to load more comments.';
    } finally {
      loadingMore = false;
    }
  }

  $: totalPages = Math.max(1, Math.ceil(commentTotal / perPage));
</script>

<article class="post-page">
  <section class="card-surface post-hero">
    {#if mediaUrl}
      <img src={mediaUrl} alt={post.title} loading="lazy" decoding="async" />
    {/if}
    <div class="meta">
      <span class={`badge visibility ${post.visibility}`}>{post.visibility}</span>
      <h1>{post.title}</h1>
      <p class="timestamp">Published {formatDate(post.created_at)}</p>
      {#if post.description}
        <p class="description">{post.description}</p>
      {/if}
      <ul class="details">
        <li><strong>Owner:</strong> {post.owner_id}</li>
        {#if post.like_count !== undefined}
          <li><strong>Likes:</strong> {post.like_count}</li>
        {/if}
        {#if post.content_type}
          <li><strong>Type:</strong> {post.content_type}</li>
        {/if}
        {#if post.size_bytes}
          <li><strong>Size:</strong> {(post.size_bytes / 1024).toFixed(1)} KB</li>
        {/if}
        {#if post.width && post.height}
          <li><strong>Dimensions:</strong> {post.width} × {post.height}</li>
        {/if}
      </ul>
      {#if post.tags?.length}
        <ul class="tags">
          {#each post.tags as tag}
            <li>#{tag}</li>
          {/each}
        </ul>
      {/if}
    </div>
  </section>

  <section class="card-surface comments">
    <header>
      <h2>Comments ({commentTotal})</h2>
      <p class="subtitle">Join the conversation. You must be signed in to post.</p>
    </header>
    <form class="comment-form" on:submit={handleComment}>
      <label class="visually-hidden" for="comment-input">Comment text</label>
      <textarea
        id="comment-input"
        rows="4"
        placeholder={authState ? 'Write your comment…' : 'Log in to post a comment'}
        bind:value={commentText}
        disabled={!authState || submitting}
        required
      ></textarea>
      {#if !authState}
        <p class="hint">
          <a href="/login">Log in</a> to post comments and access private discussions.
        </p>
      {/if}
      {#if error}
        <p class="error" role="alert">{error}</p>
      {/if}
      <button type="submit" disabled={!authState || submitting}>
        {#if submitting}
          Posting…
        {:else}
          Post comment
        {/if}
      </button>
    </form>
    <CommentList {comments} />
    {#if totalPages > currentPage}
      <button type="button" class="load-more" on:click={loadMoreComments} disabled={loadingMore}>
        {#if loadingMore}
          Loading…
        {:else}
          Load more comments
        {/if}
      </button>
    {/if}
    {#if commentsError}
      <p class="error" role="alert">{commentsError}</p>
    {/if}
  </section>
</article>

<style>
  .post-page {
    display: flex;
    flex-direction: column;
    gap: clamp(1.5rem, 4vw, 2.5rem);
  }

  .post-hero {
    display: grid;
    grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
    gap: clamp(1.25rem, 4vw, 2rem);
    overflow: hidden;
  }

  .post-hero img {
    width: 100%;
    border-radius: 1rem;
    object-fit: cover;
    max-height: 480px;
  }

  .meta {
    display: flex;
    flex-direction: column;
    gap: 0.8rem;
  }

  .timestamp {
    color: rgba(226, 232, 240, 0.8);
    font-size: 0.95rem;
  }

  .description {
    line-height: 1.6;
    color: rgba(226, 232, 240, 0.9);
  }

  .details {
    list-style: none;
    padding: 0;
    margin: 0;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 0.5rem 1rem;
    font-size: 0.9rem;
    color: rgba(226, 232, 240, 0.8);
  }

  .details strong {
    color: rgba(148, 163, 184, 0.95);
  }

  .tags {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    list-style: none;
    padding: 0;
    margin: 0;
    color: rgba(226, 232, 240, 0.85);
  }

  .comments {
    padding: clamp(1.5rem, 4vw, 2.5rem);
  }

  .subtitle {
    color: rgba(148, 163, 184, 0.8);
    margin-top: -0.5rem;
  }

  .comment-form {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    margin: 1.25rem 0 1.75rem;
  }

  textarea {
    resize: vertical;
  }

  .error {
    color: #fca5a5;
    font-weight: 600;
  }

  .hint {
    font-size: 0.85rem;
    color: rgba(148, 163, 184, 0.8);
  }

  .hint a {
    color: rgba(96, 165, 250, 0.9);
  }

  .load-more {
    margin: 1.5rem auto 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding-inline: 2rem;
  }

  @media (max-width: 960px) {
    .post-hero {
      grid-template-columns: 1fr;
    }

    .post-hero img {
      max-height: none;
    }
  }
</style>
