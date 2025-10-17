import { browser } from '$app/environment';
import { writable } from 'svelte/store';
import { logout as apiLogout } from '$lib/api/client';
import type { AuthTokens } from '$lib/api/types';

type Fetch = typeof fetch;

const STORAGE_KEY = 'fonudin_auth_state';

export interface AuthState extends AuthTokens {
  /**
   * Epoch timestamp (ms) when the access token expires.
   */
  expires_at: number;
}

type AuthStore = ReturnType<typeof createAuthStore>;

function readFromStorage(): AuthState | null {
  if (!browser) return null;
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw) as AuthState;
    if (!parsed || typeof parsed.access_token !== 'string') {
      return null;
    }
    return parsed;
  } catch (error) {
    console.warn('Failed to parse auth state from storage', error);
    return null;
  }
}

function createAuthStore() {
  const initial = readFromStorage();
  const { subscribe, set } = writable<AuthState | null>(initial);
  let current: AuthState | null = initial;

  subscribe((value) => {
    current = value;
    if (!browser) return;

    if (value) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  });

  return {
    subscribe,
    setTokens(tokens: AuthTokens) {
      const expires_at = Date.now() + tokens.expires_in * 1000;
      set({ ...tokens, expires_at });
    },
    clear() {
      set(null);
    },
    get snapshot() {
      return current;
    },
    isExpired() {
      if (!current) return true;
      return current.expires_at <= Date.now();
    },
    async logout(customFetch?: Fetch) {
      if (current) {
        try {
          const activeFetch = customFetch ?? (typeof fetch !== 'undefined' ? fetch : undefined);
          if (activeFetch) {
            await apiLogout(activeFetch, current.access_token);
          }
        } catch (error) {
          console.warn('Logout request failed', error);
        }
      }
      set(null);
    }
  } satisfies {
    subscribe: typeof subscribe;
    setTokens: (tokens: AuthTokens) => void;
    clear: () => void;
    snapshot: AuthState | null;
    isExpired: () => boolean;
    logout: (customFetch?: Fetch) => Promise<void>;
  };
}

export const authStore: AuthStore = createAuthStore();
