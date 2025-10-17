<script lang="ts">
  import type { Comment } from '$lib/api/types';
  import { formatDate } from '$lib/api/client';

  export let comments: Comment[] = [];
</script>

<ul class="comment-list" aria-live="polite">
  {#if comments.length === 0}
    <li class="empty">No comments yet. Be the first to share your thoughts.</li>
  {:else}
    {#each comments as comment (comment.id)}
      <li class="card-surface comment">
        <div class="meta">
          <span class="author">{comment.author_id}</span>
          <span aria-hidden="true">•</span>
          <span>{formatDate(comment.created_at)}</span>
        </div>
        <p>{comment.text}</p>
      </li>
    {/each}
  {/if}
</ul>

<style>
  .comment-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    padding: 0;
    margin: 0;
    list-style: none;
  }

  .comment {
    padding: 1rem 1.25rem;
  }

  .meta {
    display: flex;
    gap: 0.4rem;
    align-items: center;
    font-size: 0.85rem;
    color: rgba(226, 232, 240, 0.75);
    margin-bottom: 0.6rem;
  }

  .author {
    font-weight: 600;
    color: rgba(96, 165, 250, 0.9);
  }

  .empty {
    text-align: center;
    color: rgba(226, 232, 240, 0.8);
    padding: 2rem 1.5rem;
    border: 1px dashed rgba(148, 163, 184, 0.4);
    border-radius: 1rem;
  }
</style>
