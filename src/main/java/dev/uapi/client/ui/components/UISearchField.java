package dev.uapi.client.ui.components;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/** Semantic text field for client-side filtering. */
public final class UISearchField extends UITextField {
    public UISearchField(Component placeholder, int maxLength, Consumer<String> onQueryChanged) {
        super(placeholder, maxLength, onQueryChanged);
    }
}
