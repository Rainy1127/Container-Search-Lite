package dev.mihail.containersearchlite.search;

/** What a single search term is matched against. */
public enum TermKind {
    /** Plain text: the item's display name or its registry path. */
    NAME,
    /** {@code @text}: the mod namespace or the mod's display name. */
    MOD,
    /** {@code #text}: the identifiers of the tags the item belongs to. */
    TAG,
    /** {@code $text}: the lines of the item tooltip. */
    TOOLTIP
}
