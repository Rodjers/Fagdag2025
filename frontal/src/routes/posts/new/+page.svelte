<script lang="ts">
  import { goto } from '$app/navigation';
  import { ApiError, createPost } from '$lib/api/client';
  import type { AuthState } from '$lib/stores/auth';
  import { authStore } from '$lib/stores/auth';
  import { onDestroy } from 'svelte';

  let title = '';
  let description = '';
  let tagsInput = '';
  let visibility: 'public' | 'unlisted' | 'private' = 'public';
  let file: File | null = null;
  let selectedFileName = '';
  let submitting = false;
  let error: string | null = null;
  let authState: AuthState | null = authStore.snapshot;

  const unsubscribe = authStore.subscribe((value) => {
    authState = value;
  });

  onDestroy(() => unsubscribe());

  function handleFileChange(event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      file = input.files[0];
      selectedFileName = file.name;
    } else {
      file = null;
      selectedFileName = '';
    }
  }

  async function handleSubmit(event: SubmitEvent) {
    event.preventDefault();

    if (!authState) {
      error = 'Please log in to create a new post.';
      return;
    }

    if (!file) {
      error = 'Select a file to upload before publishing your post.';
      return;
    }

    submitting = true;
    error = null;

    const tags = tagsInput
      .split(',')
      .map((tag) => tag.trim())
      .filter((tag, index, all) => tag.length > 0 && all.indexOf(tag) === index);

    try {
      const post = await createPost(
        fetch,
        {
          file,
          title: title.trim() || undefined,
          description: description.trim() || undefined,
          tags,
          visibility
        },
        authState.access_token
      );

      goto(`/posts/${post.id}`, { replaceState: true });
    } catch (err) {
      console.error(err);
      if (err instanceof ApiError) {
        error = err.data?.message ?? 'Failed to create the post. Please try again.';
      } else if (err instanceof Error) {
        error = err.message;
      } else {
        error = 'Failed to create the post. Please try again.';
      }
    } finally {
      submitting = false;
    }
  }
</script>

<section class="create-post card-surface">
  <h1>Upload a new post</h1>
  <p class="lead">
    Share your latest creation with the OnlyPikks community. Provide a descriptive title, optional summary and
    tags to help others discover your work.
  </p>

  {#if !authState}
    <p class="auth-hint">
      You must <a href="/login">log in</a> before you can publish a post.
    </p>
  {/if}

  <form class="create-form" on:submit={handleSubmit}>
    <fieldset disabled={submitting}>
      <label>
        <span>Media file</span>
        <input type="file" required on:change={handleFileChange} accept="image/*,video/*" />
        <small>Select an image or video to associate with your post.</small>
        {#if selectedFileName}
          <p class="file-name">Selected: {selectedFileName}</p>
        {/if}
      </label>

      <label>
        <span>Title</span>
        <input type="text" placeholder="Untitled post" bind:value={title} maxlength={120} />
      </label>

      <label>
        <span>Description</span>
        <textarea rows="4" placeholder="Describe your upload" bind:value={description} maxlength={800}></textarea>
      </label>

      <label>
        <span>Tags</span>
        <input
          type="text"
          placeholder="e.g. landscape, sunset, hdr"
          bind:value={tagsInput}
          autocomplete="off"
        />
        <small>Separate tags with commas. Duplicates are removed automatically.</small>
      </label>

      <label>
        <span>Visibility</span>
        <select bind:value={visibility}>
          <option value="public">Public</option>
          <option value="unlisted">Unlisted</option>
          <option value="private">Private</option>
        </select>
      </label>
    </fieldset>

    {#if error}
      <p class="error" role="alert">{error}</p>
    {/if}

    <div class="actions">
      <button type="submit" disabled={!authState || submitting}>
        {#if submitting}
          Publishing…
        {:else}
          Publish post
        {/if}
      </button>
      <a class="cancel" href="/">Cancel</a>
    </div>
  </form>
</section>

<style>
  .create-post {
    margin: 2rem auto;
    max-width: 720px;
    padding: clamp(1.75rem, 4vw, 2.5rem);
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .lead {
    color: rgba(226, 232, 240, 0.85);
    margin: 0;
  }

  .auth-hint {
    background: rgba(59, 130, 246, 0.15);
    border: 1px solid rgba(59, 130, 246, 0.35);
    border-radius: 0.85rem;
    padding: 0.75rem 1rem;
    color: rgba(226, 232, 240, 0.9);
  }

  .auth-hint a {
    color: rgba(96, 165, 250, 0.95);
    font-weight: 600;
  }

  .create-form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  fieldset {
    border: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
  }

  label {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    color: rgba(226, 232, 240, 0.85);
  }

  label span {
    font-weight: 600;
    letter-spacing: 0.04em;
  }

  input[type='file'] {
    background: rgba(15, 23, 42, 0.75);
    border-radius: 0.75rem;
    border: 1px solid rgba(148, 163, 184, 0.3);
    padding: 0.75rem 1rem;
  }

  input[type='file']::file-selector-button {
    border: none;
    border-radius: 0.6rem;
    padding: 0.5rem 1rem;
    margin-right: 1rem;
    cursor: pointer;
    background: rgba(59, 130, 246, 0.85);
    color: #f8fafc;
  }

  small {
    font-size: 0.8rem;
    color: rgba(148, 163, 184, 0.8);
  }

  .file-name {
    font-size: 0.85rem;
    color: rgba(148, 163, 184, 0.85);
  }

  textarea {
    min-height: 160px;
    resize: vertical;
  }

  .actions {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .actions .cancel {
    color: rgba(148, 163, 184, 0.85);
    font-weight: 600;
  }

  .error {
    color: #fca5a5;
    font-weight: 600;
    margin: 0;
  }
</style>
