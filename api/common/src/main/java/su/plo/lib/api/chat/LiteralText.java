package su.plo.lib.api.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class LiteralText extends TextComponent {

    @Getter
    private final String text;

    @Override
    public String toString() {
        return text;
    }
}