# Neutral profile facets

U-API 2 exposes a small server-authoritative contract under `dev.uapi.api.profile`. It lets two
optional mods contribute bounded profile sections without either mod importing the other's Java
types.

## Data flow

1. A mod registers a `ProfileFacetProvider` for a live `MinecraftServer` through
   `ProfileFacetRegistry.register`.
2. A server-owned screen payload calls `ProfileFacetRegistry.query` with the viewer, subject and
   surface (`PUBLIC_PROFILE`, `PLAYER_CARD` or `INSPECTION`).
3. U-API applies the declared `ProfileFacetAudience`, sorts the result and caps it at 32 facets.
4. The owning mod encodes the result with `ProfileFacetWireCodec` in its own versioned payload.
5. The client renders `ProfileFacetText.component()` values in its own UI, or uses the retained
   `UIProfileFacetPanel`. Optional item/sprite/texture icon metadata is namespaced and bounded;
   generic rendering supports item and sprite icons while provider textures remain screen-owned.
   U-API does not open a screen or send an unsolicited packet.

Providers are server-scoped and cleaned at server stop. Duplicate provider IDs are rejected, and a
provider failure is isolated from other providers.

## Privacy boundary

`PUBLIC`, `SHARED_SOCIAL_GROUP` and `SUBJECT_ONLY` are minimum audience filters enforced centrally.
They are not a replacement for provider policy. A provider must apply any stricter setting before
returning a facet and must never put secrets in a facet that could be public.

Social providers should publish names, tags, and roles only when their own privacy policy permits
it; otherwise the facet must be subject-only. Soul Ascension publishes only the pre-existing public
subset: level, active title, and allocated stats. It does not publish free points, damage progress,
modifier sources, or server configuration.

## Current adapters and UI hooks

- `SoulProfileFacetProvider` registers automatically and shares only U-API records.
- New social/profile providers should use a separate, versioned request after a subject is selected,
  re-check permissions and privacy on the server, query the appropriate context, encode the bounded
  response with `ProfileFacetWireCodec`, and render it in their own detail panel.

Those payload/UI hooks are intentionally owned by their respective screens. The provider contract
does not change an existing wire schema behind the screen's back.
