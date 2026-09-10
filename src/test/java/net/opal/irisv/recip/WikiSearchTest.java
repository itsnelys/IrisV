package net.opal.irisv.recip;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WikiSearchTest {
    @Test void ignoresCaseAndAccents() {
        assertTrue(WikiSearch.matches("EPINGLEE", "Recette épinglée"));
    }
    @Test void matchesEveryWordRegardlessOfOrder() {
        assertTrue(WikiSearch.matches("recette transfert", "Transfert des ingrédients de la recette"));
        assertFalse(WikiSearch.matches("recette four", "Recette de table de craft"));
    }
    @Test void blankQueryMatchesAndPunctuationIsLiteral() {
        assertTrue(WikiSearch.matches("  ", "HUD"));
        assertTrue(WikiSearch.matches("@mod", "Recherche @mod"));
        assertFalse(WikiSearch.matches(".*", "HUD"));
    }
}
