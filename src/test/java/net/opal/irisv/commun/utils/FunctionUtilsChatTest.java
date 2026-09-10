package net.opal.irisv.commun.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FunctionUtilsChatTest {
    @Test void repeatedMessagesAreThrottledButDifferentActionsAreNot() {
        assertTrue(FunctionUtilsChat.allowClientMessage("repeat", 0));
        assertFalse(FunctionUtilsChat.allowClientMessage("repeat", 500_000_000));
        assertTrue(FunctionUtilsChat.allowClientMessage("different", 500_000_000));
        assertTrue(FunctionUtilsChat.allowClientMessage("repeat", 1_000_000_000));
    }

    @Test void limiterMemoryIsBounded() {
        assertTrue(FunctionUtilsChat.allowClientMessage("oldest", 0));
        for (int i = 0; i < 65; i++) FunctionUtilsChat.allowClientMessage("bounded-" + i, 0);
        assertTrue(FunctionUtilsChat.allowClientMessage("oldest", 1));
    }

    @Test void noPlayerDoesNotLoadConfigOrSendAnything() {
        assertDoesNotThrow(() -> FunctionUtilsChat.clientAction(null, "test"));
        assertDoesNotThrow(() -> FunctionUtilsChat.clientError(null, "test"));
    }
}
