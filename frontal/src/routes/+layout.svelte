<script lang="ts">
  import '../app.css';
  import favicon from '$lib/assets/favicon.svg';
  import { authStore } from '$lib/stores/auth';
  import { userStore } from '$lib/stores/user';
  import { goto } from '$app/navigation';

  const handleLogout = async () => {
    await authStore.logout(fetch);
    goto('/');
  };
</script>

<svelte:head>
  <link rel="icon" href={favicon} />
</svelte:head>

<div class="app-shell">
  <header class="card-surface app-header">
    <a class="brand" href="/">
      <span class="logo" aria-hidden="true">OnlyPikks</span>
      <span class="title">OnlyPikks</span>
    </a>
    <nav>
      <a href="/">Feed</a>
      <a href="/?sort=popular">Popular</a>
      <a href="/?sort=trending">Trending</a>
      {#if $authStore}
        <a href="/posts/new" class="create-post">Create post</a>
        <a href="/?owner=me">My posts</a>
      {/if}
    </nav>
    <div class="auth-controls">
      {#if $authStore}
        {#if $userStore}
          <span class="user-hint">Signed in as {$userStore.name}</span>
        {/if}
        <span class="token-hint">Token expires {new Date($authStore.expires_at).toLocaleTimeString()}</span>
        <button type="button" on:click={handleLogout}>Log out</button>
      {:else}
        <a class="login" href="/login">Log in</a>
      {/if}
    </div>
  </header>

  <main>
    <slot />
  </main>

  <footer class="app-footer">
    <p>
      Frontend scaffold for the Posts Service API.
      <a href="https://localhost:8080/health" rel="noreferrer">API health</a>
    </p>
  </footer>
</div>

<style>
  .app-shell {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }

  .app-header {
    margin: 2rem auto 0;
    padding: clamp(1rem, 4vw, 1.5rem) clamp(1.25rem, 4vw, 2rem);
    width: min(1100px, 100% - clamp(1rem, 5vw, 2rem));
    display: flex;
    align-items: center;
    gap: 1.5rem;
    flex-wrap: wrap;
  }

  .brand {
    display: flex;
    align-items: baseline;
    gap: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.12em;
    font-weight: 700;
    font-size: 0.95rem;
    color: rgba(148, 163, 184, 0.9);
  }

  .logo {
    font-size: 0.85rem;
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.85), rgba(236, 72, 153, 0.85));
    padding: 0.35rem 0.6rem;
    border-radius: 0.6rem;
    color: #f8fafc;
  }

  .title {
    letter-spacing: 0.08em;
  }

  nav {
    display: flex;
    gap: 1rem;
    flex-grow: 1;
    flex-wrap: wrap;
  }

  nav a {
    padding: 0.35rem 0.75rem;
    border-radius: 999px;
    background: rgba(148, 163, 184, 0.16);
    border: 1px solid transparent;
    transition: border-color 0.2s ease, background 0.2s ease;
  }

  nav a:hover,
  nav a:focus-visible {
    background: rgba(59, 130, 246, 0.25);
    border-color: rgba(59, 130, 246, 0.45);
  }

  nav a.create-post {
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.9), rgba(236, 72, 153, 0.9));
    border-color: transparent;
    color: #f8fafc;
    font-weight: 600;
  }

  nav a.create-post:hover,
  nav a.create-post:focus-visible {
    background: linear-gradient(135deg, rgba(59, 130, 246, 1), rgba(236, 72, 153, 1));
    box-shadow: 0 12px 24px rgba(59, 130, 246, 0.35);
  }

  .auth-controls {
    display: flex;
    align-items: center;
    gap: 0.85rem;
    margin-left: auto;
  }

  .auth-controls .login {
    padding: 0.5rem 1.1rem;
    border-radius: 0.75rem;
    background: rgba(96, 165, 250, 0.25);
    border: 1px solid rgba(96, 165, 250, 0.35);
  }

  .token-hint {
    font-size: 0.8rem;
    color: rgba(226, 232, 240, 0.75);
  }

  .user-hint {
    font-size: 0.85rem;
    color: rgba(226, 232, 240, 0.85);
    font-weight: 600;
  }

  main {
    flex: 1 1 auto;
    width: 100%;
  }

  .app-footer {
    width: min(1100px, 100% - clamp(1rem, 5vw, 2rem));
    margin: 0 auto 2rem;
    color: rgba(148, 163, 184, 0.75);
    font-size: 0.9rem;
  }

  .app-footer a {
    color: rgba(96, 165, 250, 0.85);
  }
</style>
