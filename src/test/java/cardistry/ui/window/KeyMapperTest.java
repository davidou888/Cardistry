package cardistry.ui.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class KeyMapperTest {

    private static final JPanel SOURCE = new JPanel();

    private static KeyEvent pressed(int code, int modifiers) {
        return new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, 0, modifiers, code, KeyEvent.CHAR_UNDEFINED);
    }

    private static KeyEvent typed(char c, int modifiers) {
        return new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, 0, modifiers, KeyEvent.VK_UNDEFINED, c);
    }

    @Test
    void touchesSpeciales() {
        assertEquals(new KeyStroke(KeyType.ArrowLeft), KeyMapper.pressed(pressed(KeyEvent.VK_LEFT, 0)));
        assertEquals(new KeyStroke(KeyType.ArrowRight), KeyMapper.pressed(pressed(KeyEvent.VK_RIGHT, 0)));
        assertEquals(new KeyStroke(KeyType.ArrowUp), KeyMapper.pressed(pressed(KeyEvent.VK_UP, 0)));
        assertEquals(new KeyStroke(KeyType.ArrowDown), KeyMapper.pressed(pressed(KeyEvent.VK_DOWN, 0)));
        assertEquals(new KeyStroke(KeyType.Enter), KeyMapper.pressed(pressed(KeyEvent.VK_ENTER, 0)));
        assertEquals(new KeyStroke(KeyType.Escape), KeyMapper.pressed(pressed(KeyEvent.VK_ESCAPE, 0)));
        assertNull(KeyMapper.pressed(pressed(KeyEvent.VK_A, 0)), "une lettre vient de keyTyped");
    }

    @Test
    void ctrlZ() {
        KeyStroke key = KeyMapper.pressed(pressed(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        assertEquals(new KeyStroke('z', true, false), key);
        assertNull(KeyMapper.typed(typed((char) 26, InputEvent.CTRL_DOWN_MASK)), "pas de doublon");
    }

    @Test
    void caracteres() {
        assertEquals(new KeyStroke('p', false, false), KeyMapper.typed(typed('p', 0)));
        assertEquals(new KeyStroke('é', false, false), KeyMapper.typed(typed('é', 0)));
        assertEquals(new KeyStroke('R', false, false, true), KeyMapper.typed(typed('R', InputEvent.SHIFT_DOWN_MASK)));
        assertNull(KeyMapper.typed(typed('\n', 0)), "Entrée vient de keyPressed");
    }

    @Test
    void altGrGardeLeCaractere() {
        assertEquals(new KeyStroke('@', false, false),
                KeyMapper.typed(typed('@', InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)));
    }
}
