package dev.oblac.gart

import java.awt.event.KeyEvent.*

enum class Key(val platformKeyCode: Int) {
    KEY_UNKNOWN(-1),
    KEY_A(VK_A),
    KEY_S(VK_S),
    KEY_D(VK_D),
    KEY_F(VK_F),
    KEY_H(VK_H),
    KEY_G(VK_G),
    KEY_Z(VK_Z),
    KEY_X(VK_X),
    KEY_C(VK_C),
    KEY_V(VK_V),
    KEY_B(VK_B),
    KEY_Q(VK_Q),
    KEY_W(VK_W),
    KEY_E(VK_E),
    KEY_R(VK_R),
    KEY_Y(VK_Y),
    KEY_T(VK_T),
    KEY_U(VK_U),
    KEY_I(VK_I),
    KEY_P(VK_P),
    KEY_L(VK_L),
    KEY_J(VK_J),
    KEY_K(VK_K),
    KEY_N(VK_N),
    KEY_M(VK_M),
    KEY_O(VK_O),
    KEY_1(VK_1),
    KEY_2(VK_2),
    KEY_3(VK_3),
    KEY_4(VK_4),
    KEY_5(VK_5),
    KEY_6(VK_6),
    KEY_7(VK_7),
    KEY_8(VK_8),
    KEY_9(VK_9),
    KEY_0(VK_0),
    KEY_CLOSE_BRACKET(VK_CLOSE_BRACKET),
    KEY_OPEN_BRACKET(VK_OPEN_BRACKET),
    KEY_QUOTE(VK_QUOTE),
    KEY_SEMICOLON(VK_SEMICOLON),
    KEY_SLASH(VK_SLASH),
    KEY_COMMA(VK_COMMA),
    KEY_BACKSLASH(VK_BACK_SLASH),
    KEY_PERIOD(VK_PERIOD),
    KEY_BACK_QUOTE(VK_BACK_QUOTE),
    KEY_EQUALS(VK_EQUALS),
    KEY_MINUS(VK_MINUS),
    KEY_ENTER(VK_ENTER),
    KEY_ESCAPE(VK_ESCAPE),
    KEY_TAB(VK_TAB),
    KEY_BACKSPACE(VK_BACK_SPACE),
    KEY_SPACE(VK_SPACE),
    KEY_CAPSLOCK(VK_CAPS_LOCK),
    KEY_LEFT_META(VK_META),
    KEY_LEFT_SHIFT(VK_SHIFT),
    KEY_LEFT_ALT(VK_ALT),
    KEY_LEFT_CONTROL(VK_CONTROL),
    KEY_RIGHT_META(0x80000000.toInt() or VK_META),
    KEY_RIGHT_SHIFT(0x80000000.toInt() or VK_SHIFT),
    KEY_RIGHT_ALT(VK_ALT_GRAPH),
    KEY_RIGHT_CONTROL(0x80000000.toInt() or VK_CONTROL),
    KEY_MENU(VK_CONTEXT_MENU),
    KEY_UP(VK_UP),
    KEY_DOWN(VK_DOWN),
    KEY_LEFT(VK_LEFT),
    KEY_RIGHT(VK_RIGHT),
    KEY_F1(VK_F1),
    KEY_F2(VK_F2),
    KEY_F3(VK_F3),
    KEY_F4(VK_F4),
    KEY_F5(VK_F5),
    KEY_F6(VK_F6),
    KEY_F7(VK_F7),
    KEY_F8(VK_F8),
    KEY_F9(VK_F9),
    KEY_F10(VK_F10),
    KEY_F11(VK_F11),
    KEY_F12(VK_F12),
    KEY_PRINTSCEEN(VK_PRINTSCREEN),
    KEY_SCROLL_LOCK(VK_SCROLL_LOCK),
    KEY_PAUSE(VK_PAUSE),
    KEY_INSERT(VK_INSERT),
    KEY_HOME(VK_HOME),
    KEY_PGUP(VK_PAGE_UP),
    KEY_DELETE(VK_DELETE),
    KEY_END(VK_END),
    KEY_PGDOWN(VK_PAGE_DOWN),
    KEY_NUM_LOCK(VK_NUM_LOCK),
    KEY_NUMPAD_0(VK_NUMPAD0),
    KEY_NUMPAD_1(VK_NUMPAD1),
    KEY_NUMPAD_2(VK_NUMPAD2),
    KEY_NUMPAD_3(VK_NUMPAD3),
    KEY_NUMPAD_4(VK_NUMPAD4),
    KEY_NUMPAD_5(VK_NUMPAD5),
    KEY_NUMPAD_6(VK_NUMPAD6),
    KEY_NUMPAD_7(VK_NUMPAD7),
    KEY_NUMPAD_8(VK_NUMPAD8),
    KEY_NUMPAD_9(VK_NUMPAD9),
    KEY_NUMPAD_ENTER(0x80000000.toInt() or VK_ENTER),
    KEY_NUMPAD_ADD(VK_ADD),
    KEY_NUMPAD_SUBTRACT(VK_SUBTRACT),
    KEY_NUMPAD_MULTIPLY(VK_MULTIPLY),
    KEY_NUMPAD_DIVIDE(VK_DIVIDE),
    KEY_NUMPAD_DECIMAL(VK_DECIMAL);

    companion object {
        fun valueOf(platformKeyCode: Int): Key {
            return entries.firstOrNull { it.platformKeyCode == platformKeyCode } ?: KEY_UNKNOWN
        }
    }
}