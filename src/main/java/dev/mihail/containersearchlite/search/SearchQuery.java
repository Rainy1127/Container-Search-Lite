package dev.mihail.containersearchlite.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A parsed search box input. Pure Java: no Minecraft classes are referenced here.
 *
 * <h2>Syntax</h2>
 * The input is split on whitespace into terms. A term matches an item when it is found in:
 * <ul>
 *     <li>{@code text} - the display name or the registry path ({@code oak_log} and {@code oak log} both work)</li>
 *     <li>{@code @text} - the mod: namespace or display name ({@code @minecraft}, {@code @create})</li>
 *     <li>{@code #text} - a tag the item is in ({@code #logs}, {@code #minecraft:planks})</li>
 *     <li>{@code $text} - the tooltip, e.g. enchantments or lore ({@code $sharpness})</li>
 * </ul>
 * All terms must match (logical AND), so {@code @minecraft #logs} finds vanilla logs.
 */
public final class SearchQuery {
    /** The query that matches nothing and highlights nothing. */
    public static final SearchQuery EMPTY = new SearchQuery(List.of());

    /** Upper bound on terms, so a pathological input cannot make matching slow. */
    static final int MAX_TERMS = 8;

    private final List<SearchTerm> terms;
    private final boolean needsTooltip;

    private SearchQuery(List<SearchTerm> terms) {
        this.terms = List.copyOf(terms);
        boolean tooltip = false;
        for (SearchTerm term : this.terms) {
            if (term.kind() == TermKind.TOOLTIP) {
                tooltip = true;
                break;
            }
        }
        this.needsTooltip = tooltip;
    }

    /**
     * Parses raw user input. Never throws; blank input and bare prefixes (a lone {@code @} while the user is still
     * typing) simply produce no term.
     */
    public static SearchQuery parse(String raw) {
        if (raw == null) {
            return EMPTY;
        }
        String trimmed = raw.strip();
        if (trimmed.isEmpty()) {
            return EMPTY;
        }

        List<SearchTerm> terms = new ArrayList<>();
        for (String token : trimmed.split("\\s+")) {
            if (terms.size() >= MAX_TERMS) {
                break;
            }
            SearchTerm term = parseToken(token);
            if (term != null) {
                terms.add(term);
            }
        }
        return terms.isEmpty() ? EMPTY : new SearchQuery(terms);
    }

    private static SearchTerm parseToken(String token) {
        if (token.isEmpty()) {
            return null;
        }
        char prefix = token.charAt(0);
        TermKind kind = switch (prefix) {
            case '@' -> TermKind.MOD;
            case '#' -> TermKind.TAG;
            case '$' -> TermKind.TOOLTIP;
            default -> TermKind.NAME;
        };
        String text = kind == TermKind.NAME ? token : token.substring(1);
        text = text.toLowerCase(Locale.ROOT);
        if (kind == TermKind.NAME) {
            // Registry paths are normalised to spaces, so "oak_log" must become "oak log" to match them.
            text = text.replace('_', ' ');
        }
        return text.isEmpty() ? null : new SearchTerm(kind, text);
    }

    public boolean isEmpty() {
        return terms.isEmpty();
    }

    public List<SearchTerm> terms() {
        return terms;
    }

    /** Whether evaluating this query needs {@link ItemFacts#tooltipText()}. */
    public boolean needsTooltip() {
        return needsTooltip;
    }

    /**
     * Tests an item against every term. An empty query matches nothing, because "no search" must not highlight
     * the whole container.
     *
     * @param allowTooltip when {@code false}, {@code $} terms never match (the tooltip feature is switched off)
     */
    public boolean matches(ItemFacts facts, boolean allowTooltip) {
        if (terms.isEmpty()) {
            return false;
        }
        for (SearchTerm term : terms) {
            if (!matches(term, facts, allowTooltip)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(SearchTerm term, ItemFacts facts, boolean allowTooltip) {
        String text = term.text();
        return switch (term.kind()) {
            case NAME -> facts.displayName().contains(text) || facts.idPath().contains(text);
            case MOD -> facts.namespace().contains(text) || facts.modName().contains(text);
            case TAG -> facts.hasTagContaining(text);
            case TOOLTIP -> allowTooltip && facts.tooltipText().contains(text);
        };
    }
}
