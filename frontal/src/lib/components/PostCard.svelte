<script lang="ts">
  import type { PostSummary } from '$lib/api/types';
  import { formatDate } from '$lib/api/client';

  export let post: PostSummary;
</script>

<article class="card-surface post-card">
  <a class="media" href={`/posts/${post.id}`} aria-label={`Open ${post.title}`}>
    {#if post.thumbnail_url}
      <img src={post.thumbnail_url} alt={post.title} loading="lazy" decoding="async" />
    {:else}
      <div class="placeholder" aria-hidden="true">
        <span>{post.title.slice(0, 1).toUpperCase()}</span>
      </div>
    {/if}
  </a>
  <div class="content">
    <div class="header">
      <span class={`badge visibility ${post.visibility}`}>{post.visibility}</span>
      {#if post.like_count !== undefined}
        <span class="meta">❤️ {post.like_count}</span>
      {/if}
    </div>
    <h3>
      <a href={`/posts/${post.id}`}>{post.title}</a>
    </h3>
    <p class="meta">
      by <span class="emphasis">{post.owner_id}</span>
      <span aria-hidden="true">•</span>
      {formatDate(post.created_at)}
    </p>
    {#if post.tags?.length}
      <ul class="tags" aria-label="Tags">
        {#each post.tags as tag}
          <li>#{tag}</li>
        {/each}
      </ul>
    {/if}
  </div>
</article>

<style>
  .post-card {
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .media {
    position: relative;
    aspect-ratio: 4 / 3;
    overflow: hidden;
    display: block;
  }

  .media img,
  .placeholder {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .placeholder {
    display: grid;
    place-items: center;
    background: rgba(15, 23, 42, 0.85);
    font-size: 2.5rem;
    font-weight: 600;
    letter-spacing: 0.1em;
    color: rgba(248, 250, 252, 0.75);
  }

  .content {
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
  }

  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
  }

  .meta {
    font-size: 0.9rem;
    color: rgba(226, 232, 240, 0.85);
  }

  .emphasis {
    color: rgba(96, 165, 250, 0.9);
    font-weight: 600;
  }

  .tags {
    display: flex;
    gap: 0.4rem;
    flex-wrap: wrap;
    list-style: none;
    margin: 0;
    padding: 0;
    font-size: 0.8rem;
    color: rgba(226, 232, 240, 0.75);
  }

  .visibility {
    font-weight: 600;
    letter-spacing: 0.06em;
  }

  .visibility.public {
    background: rgba(34, 197, 94, 0.2);
    border-color: rgba(34, 197, 94, 0.4);
  }

  .visibility.unlisted {
    background: rgba(249, 115, 22, 0.2);
    border-color: rgba(249, 115, 22, 0.4);
  }

  .visibility.private {
    background: rgba(99, 102, 241, 0.2);
    border-color: rgba(99, 102, 241, 0.4);
  }
</style>
