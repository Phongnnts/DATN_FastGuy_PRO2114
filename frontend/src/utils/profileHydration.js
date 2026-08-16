export function normalizeProfile(data, fallbackRole) {
  return {
    id: data.userId,
    fullName: data.fullName || '',
    email: data.email || '',
    phone: data.phone || '',
    avatarUrl: data.avatarUrl || '',
    role: data.role || fallbackRole,
    status: data.status || '',
    loyaltyPoints: Number(data.loyaltyPoints || 0),
    createdAt: data.createdAt || null,
  };
}

export function createProfileHydrationController({ getSession, requestProfile, applyProfile, persist }) {
  let requestGeneration = 0;

  function invalidate() {
    requestGeneration += 1;
  }

  async function hydrate() {
    const session = getSession();
    if (!session.token || !session.userId) throw new Error('Chưa đăng nhập');
    const generation = ++requestGeneration;
    const data = await requestProfile();
    const current = getSession();
    if (generation !== requestGeneration || current.token !== session.token || current.userId !== session.userId || current.generation !== session.generation) return null;
    const profile = normalizeProfile(data, current.role);
    applyProfile(profile);
    persist();
    return profile;
  }

  return { hydrate, invalidate };
}

export function createProfileLoadController({ hydrate, apply, fail, setLoading, reset = () => {} }) {
  let generation = 0;
  let stopped = false;

  async function load() {
    const requestGeneration = ++generation;
    setLoading(true);
    reset();
    try {
      const profile = await hydrate();
      if (!stopped && requestGeneration === generation && profile) apply(profile);
    } catch (error) {
      if (!stopped && requestGeneration === generation) fail(error);
    } finally {
      if (!stopped && requestGeneration === generation) setLoading(false);
    }
  }

  function stop() {
    stopped = true;
    generation += 1;
  }

  return { load, stop };
}
