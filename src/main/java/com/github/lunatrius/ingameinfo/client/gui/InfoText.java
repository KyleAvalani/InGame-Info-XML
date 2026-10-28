package com.github.lunatrius.ingameinfo.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.github.lunatrius.ingameinfo.Alignment;
import com.github.lunatrius.ingameinfo.InGameInfoCore;
import com.github.lunatrius.ingameinfo.value.Value;

public class InfoText extends Info {

    private static final String ICON_START = "{ICON|";
    private final Map<String, Info> attachedValues = new LinkedHashMap<>();
    private final List<Info> usedValues = new ArrayList<>();
    private String text;
    private final List<Value> values;
    private final Alignment alignment;
    private int index;
    private boolean isActive;

    public InfoText(Alignment alignment, List<Value> values) {
        super(0, 0);
        this.values = values;
        this.alignment = alignment;
        for (Value value : values) {
            value.setParent(this);
        }
    }

    public void update(int index) {
        this.index = index;
        usedValues.clear();
        StringBuilder builder = new StringBuilder();
        for (Value value : this.values) {
            builder.append(getValue(value));
        }

        isActive = builder.length() > 0;
        updateChildren(builder);
        text = builder.toString();
        updatePosition();
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public void drawInfo() {
        if (!isActive) return;
        fontRenderer.drawStringWithShadow(text, getX(), getY(), 0x00FFFFFF);

        for (Info child : attachedValues.values()) {
            child.offsetX = x;
            child.offsetY = y;
            child.draw();
        }
    }

    private void updateChildren(StringBuilder builder) {
        for (Info child : attachedValues.values()) {
            child.hasPosition = false;
        }

        // Icons are paired with the markers in the text in the order their values were
        // evaluated during this update, which is the order they registered themselves
        // in usedValues. Icons from segments that no longer render never register, so
        // any attached value that was not used in this update is dropped below instead
        // of being drawn forever at the position of some earlier line layout.
        int usedIndex = 0;
        while (builder.indexOf(ICON_START) >= 0) {
            int iconStart = builder.indexOf(ICON_START);
            int widthStart = builder.indexOf("|", iconStart) + 1;
            if (usedIndex < usedValues.size()) {
                Info child = usedValues.get(usedIndex++);
                child.hasPosition = true;
                child.x = fontRenderer.getStringWidth(builder.substring(0, iconStart));
            }
            builder.replace(iconStart, widthStart, "");
            builder.deleteCharAt(builder.indexOf("}"));
        }

        attachedValues.values().removeIf(child -> !usedValues.contains(child));
    }

    private void updatePosition() {
        int scaledWidth = InGameInfoCore.INSTANCE.scaledWidth;
        int scaledHeight = InGameInfoCore.INSTANCE.scaledHeight;
        x = alignment.getX(scaledWidth, fontRenderer.getStringWidth(text));
        y = alignment.getY(scaledHeight, getHeight());
    }

    public @Nullable Info getAttachedValue(String tag) {
        Info value = attachedValues.get(tag);
        if (value != null && !usedValues.contains(value)) {
            usedValues.add(value);
        }
        return value;
    }

    public void removeAttachedValue(String tag) {
        Info value = attachedValues.remove(tag);
        if (value != null) {
            usedValues.remove(value);
        }
    }

    public void attachValue(@NotNull String tag, @NotNull Info value) {
        attachedValues.put(tag, value);
        if (!usedValues.contains(value)) {
            usedValues.add(value);
        }
    }

    @Override
    public int getWidth() {
        return fontRenderer.getStringWidth(text);
    }

    @Override
    public int getHeight() {
        if (alignment.ordinal() >= Alignment.BOTTOMLEFT.ordinal()) {
            return (index + 1) * (fontRenderer.FONT_HEIGHT + 1);
        }
        return index * (fontRenderer.FONT_HEIGHT + 1);
    }

    private String getValue(Value value) {
        try {
            if (value.isValidSize()) {
                return value.getReplacedValue();
            }
        } catch (Exception e) {
            return "<ERROR>";
        }

        return "";
    }

    @Override
    public String toString() {
        return String.format(
                "InfoText{text: %s, x: %d, y: %d, offsetX: %d, offsetY: %d, children: %s}",
                this.text,
                this.x,
                this.y,
                this.offsetX,
                this.offsetY,
                this.children);
    }
}
