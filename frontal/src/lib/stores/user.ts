import { writable } from 'svelte/store';
import type { UserInfo } from '$lib/api/types';

type UserStore = ReturnType<typeof createUserStore>;

function createUserStore() {
  const { subscribe, set } = writable<UserInfo | null>(null);

  return {
    subscribe,
    setUser(user: UserInfo) {
      set(user);
    },
    clear() {
      set(null);
    }
  } satisfies {
    subscribe: typeof subscribe;
    setUser: (user: UserInfo) => void;
    clear: () => void;
  };
}

export const userStore: UserStore = createUserStore();
