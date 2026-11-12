# DataMatrix

`DataMatrix` (external artifact, a ContentProvider-backed publisher/subscriber store) is the source
of truth for system state and the IPC mechanism between the console app, its services, and other
processes. Keys are enum constants in `Chimera/.../lib/common/datamatrixkeys/*.java` (`SettingKeys`,
`WorkoutKeys`, `DAPIKeys`, `PTPKeys`, …). Models wrap them with a `CallbackManager` and expose typed
listeners rather than letting callers read keys ad hoc.

## Key naming

Role prefix plus payload-type suffix, visible throughout the enums: `DATA_*` is durable state
carrying a `DataDefault<>`, `ACTION_*` is a command into a service, `EVENT_*` is a one-shot
notification out of one. `_ENUM_INT` marks an int enum ordinal; suffixes are omitted for valueless
actions and events. `DataMatrixUnitTest.java` enforces this, so a malformed key fails a test rather
than review.

## Using the library

In rough order of how often it bites:

- **Prefer `updateX` to `putX` for recurring writes.** `put` re-broadcasts to every listener in
  every process even when the value has not changed; `update` compares first and fires only on a
  real change. There is no `updateParcelable`.
- `create*ChangeAdapter` (Bool/Int/Long/Float/Double/String only) is the reader-side counterpart: it
  caches the last value and skips your callback when an unchanged one arrives. It does **not** avoid
  the IPC — the broadcast still lands — so it shields you from a noisy writer you do not control,
  while `updateX` is what stops the broadcast for everyone. Costs one extra read at registration.
- **Observe, don't poll.** Every `getX`/`putX` is a synchronous binder round trip with no client
  cache, so `getX` in a loop is N IPC calls. Register a listener and hold a local field.
- **Every `new DataMatrix()` needs a matching `finish()`** on its owner's lifecycle. A missed
  `finish()` leaks the callback, and everything it captures, for the life of the process.
- **`dm.deregister(adapter)` does not stop callbacks** — it only drops bookkeeping. Call
  `adapter.deregister()`, or `dm.finish()`.
- **Use the getter overload that takes a default.** The no-default `getX` throws NPE on an unset
  key, because unset is encoded as `MIN_VALUE` — which also means storing `MIN_VALUE` yourself is
  indistinguishable from unset.
- **One adapter per key per instance**; a second `create*Adapter` for the same key silently discards
  the callback. `CallbackManager` is how one key fans out to several listeners.
- **Callbacks queue on the main thread.** Order is therefore preserved, but a blocked main thread
  stalls every callback in the process, not just yours. The library logs `KeyDebugDelay` past
  100 ms; keep bodies short. The 3-arg `create*Adapter` fires immediately with the current value — pass `false`
  when replaying stale state is harmful. `createEventAdapter` never fires initially, and
  `createParcelableAdapter` ignores its trigger flag entirely (always fires), so use
  `CallbackManager.addParcelableListener` where that matters.
- Values are RAM-only and die with the service process unless the key is declared with
  `PersistentDataDefault`. Parcelable payloads re-serialize to every listener on every put and share
  the ~1 MB binder limit, so keep them small and non-growing.
- `DM_DEBUG_KEYS_BOOL` logs every put/get/callback with caller file and line; log bundles carry a
  `DataMatrix.json` dump.

UI modules never touch any of this directly — see the `Core/Interfaces` boundary in `CLAUDE.md`.
