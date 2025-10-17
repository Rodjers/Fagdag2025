<script lang="ts">
  import type { PostSummary } from '$lib/api/types';
  import PostCard from './PostCard.svelte';

  export let posts: PostSummary[] = [];
</script>

<section class="grid" aria-live="polite">
  {#if posts.length === 0}
    <p class="empty">No posts found. Try adjusting your filters.</p>
  {:else}
    {#each posts as post (post.id)}
      <PostCard {post} />
    {/each}
  {/if}
</section>

<style>
  .grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: clamp(1rem, 4vw, 1.75rem);
  }

  .empty {
    grid-column: 1 / -1;
    text-align: center;
    color: rgba(226, 232, 240, 0.8);
  }
</style>
