<script lang="ts">
  import { goto } from '$app/navigation';
  import { login, ApiError } from '$lib/api/client';
  import { authStore } from '$lib/stores/auth';

  let email = '';
  let password = '';
  let loading = false;
  let error: string | null = null;

  async function handleSubmit(event: SubmitEvent) {
    event.preventDefault();
    loading = true;
    error = null;

    try {
      const tokens = await login(fetch, { email, password });
      authStore.setTokens(tokens);
      goto('/', { replaceState: true });
    } catch (err) {
      console.error(err);
      if (err instanceof ApiError) {
        error = err.data?.message ?? 'Invalid credentials.';
      } else if (err instanceof Error) {
        error = err.message;
      } else {
        error = 'Unable to complete login. Please try again.';
      }
    } finally {
      loading = false;
    }
  }
</script>

<section class="login card-surface">
  <h1>Sign in</h1>
  <p class="lead">
    Authenticate with your email and password to access private or unlisted content, publish posts and manage
    your profile.
  </p>
  <form class="login-form" on:submit={handleSubmit}>
    <label>
      <span>Email address</span>
      <input type="email" required bind:value={email} autocomplete="email" />
    </label>
    <label>
      <span>Password</span>
      <input type="password" required bind:value={password} autocomplete="current-password" />
    </label>
    {#if error}
      <p class="error" role="alert">{error}</p>
    {/if}
    <button type="submit" disabled={loading}>
      {#if loading}
        Signing in…
      {:else}
        Log in
      {/if}
    </button>
  </form>
  <p class="hint">
    Looking to integrate with the API? Generate a refresh token via <code>POST /auth/login</code> and store it
    securely on the client.
  </p>
</section>

<style>
  .login {
    margin: 3rem auto;
    max-width: 480px;
    padding: clamp(1.75rem, 4vw, 2.5rem);
  }

  .lead {
    color: rgba(226, 232, 240, 0.85);
    margin-bottom: 1.5rem;
    line-height: 1.6;
  }

  .login-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
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

  .error {
    color: #fca5a5;
    font-weight: 600;
  }

  .hint {
    margin-top: 2rem;
    font-size: 0.85rem;
    color: rgba(148, 163, 184, 0.75);
  }

  code {
    background: rgba(15, 23, 42, 0.6);
    border-radius: 0.4rem;
    padding: 0.1rem 0.4rem;
  }
</style>
