/**
 * A minimal, in-memory fake of the subset of the firebase-admin Firestore
 * API this project's Cloud Functions actually use: doc().get/update/set,
 * collection().add, collection().where().count().get(), and
 * runTransaction(). Not a general-purpose Firestore emulator — just enough
 * surface area to exercise the real function handlers' logic offline, the
 * same way services.test.ts mocks the client SDK for the app's own tests.
 */

type DocData = Record<string, unknown>;

export function createFakeFirestore() {
  const store = new Map<string, DocData>();
  const addedDocs: { collection: string; data: DocData }[] = [];

  function docRef(path: string) {
    return {
      path,
      async get() {
        const data = store.get(path);
        return { exists: data !== undefined, data: () => data, id: path.split('/').pop()! };
      },
      async update(partial: DocData) {
        const existing = store.get(path);
        if (existing === undefined) throw new Error(`No document to update at ${path}`);
        store.set(path, { ...existing, ...partial });
      },
      async set(data: DocData, opts?: { merge?: boolean }) {
        const existing = store.get(path);
        store.set(path, opts?.merge && existing ? { ...existing, ...data } : data);
      },
    };
  }

  function collectionRef(name: string) {
    return {
      async add(data: DocData) {
        addedDocs.push({ collection: name, data });
        const id = `auto-${addedDocs.length}`;
        store.set(`${name}/${id}`, data);
        return { id };
      },
      where(field: string, op: string, value: unknown) {
        return {
          count() {
            return {
              async get() {
                let n = 0;
                for (const [path, data] of store.entries()) {
                  if (!path.startsWith(`${name}/`)) continue;
                  if (op === '==' && data[field] === value) n++;
                }
                return { data: () => ({ count: n }) };
              },
            };
          },
        };
      },
    };
  }

  const firestoreFn = () => ({
    doc: (path: string) => docRef(path),
    collection: (name: string) => collectionRef(name),
    async runTransaction<T>(fn: (tx: { get: (ref: ReturnType<typeof docRef>) => Promise<Awaited<ReturnType<ReturnType<typeof docRef>['get']>>>; set: (ref: ReturnType<typeof docRef>, data: DocData) => void }) => Promise<T>) {
      const tx = {
        get: (ref: ReturnType<typeof docRef>) => ref.get(),
        set: (ref: ReturnType<typeof docRef>, data: DocData) => { store.set(ref.path, data); },
      };
      return fn(tx);
    },
  });
  (firestoreFn as unknown as { FieldValue: { serverTimestamp: () => { toMillis: () => number } } }).FieldValue = {
    serverTimestamp: () => ({ toMillis: () => Date.now() }),
  };

  return {
    firestore: firestoreFn as typeof firestoreFn & { FieldValue: { serverTimestamp: () => string } },
    // Test-only helpers, not part of the real Admin SDK surface:
    _seed(path: string, data: DocData) { store.set(path, data); },
    _get(path: string) { return store.get(path); },
    _addedDocs: addedDocs,
    _reset() { store.clear(); addedDocs.length = 0; },
  };
}
