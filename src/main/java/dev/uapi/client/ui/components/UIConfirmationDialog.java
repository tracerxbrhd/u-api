package dev.uapi.client.ui.components;

import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Standard modal confirmation whose callbacks run through the same retained buttons as any screen. */
public final class UIConfirmationDialog extends UIModal {
    private final UILabel message;
    private final UIButton confirm;
    private final UIButton cancel;

    public UIConfirmationDialog(Component message, Component confirmLabel, Component cancelLabel,
                                Runnable onConfirm, Runnable onCancel) {
        super(260, 92);
        Objects.requireNonNull(onConfirm, "onConfirm");
        Objects.requireNonNull(onCancel, "onCancel");
        this.message = content().add(new UILabel(Objects.requireNonNull(message, "message"), 0xFFF4F5F8));
        this.confirm = content().add(new UIButton(Objects.requireNonNull(confirmLabel, "confirmLabel"), () -> {
            setVisible(false);
            onConfirm.run();
        }));
        this.cancel = content().add(new UIButton(Objects.requireNonNull(cancelLabel, "cancelLabel"), () -> {
            setVisible(false);
            onCancel.run();
        }));
        content().setLayout((container, bounds) -> {
            int padding = Math.min(12, bounds.width() / 2);
            this.message.setBounds(bounds.x() + padding, bounds.y() + Math.min(14, bounds.height()),
                Math.max(0, bounds.width() - padding * 2), Math.min(18, bounds.height()));
            int available = Math.max(0, bounds.width() - padding * 2);
            int buttonGap = Math.min(12, available);
            int buttonWidth = Math.max(0, (available - buttonGap) / 2);
            int buttonHeight = Math.min(22, bounds.height());
            int y = Math.max(bounds.y(), bounds.bottom() - Math.min(34, bounds.height()));
            this.confirm.setBounds(bounds.x() + padding, y, buttonWidth, buttonHeight);
            this.cancel.setBounds(bounds.right() - padding - buttonWidth, y, buttonWidth, buttonHeight);
        });
    }
}
