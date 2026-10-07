package dev.mihail.containersearchlite.search;

/**
 * One parsed search term.
 *
 * @param kind what the term is matched against
 * @param text lower-case text to look for, never empty
 */
public record SearchTerm(TermKind kind, String text) {
}
