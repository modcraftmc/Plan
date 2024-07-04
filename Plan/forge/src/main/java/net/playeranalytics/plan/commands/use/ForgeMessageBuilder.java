/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package net.playeranalytics.plan.commands.use;

import com.djrapitops.plan.commands.use.CMDSender;
import com.djrapitops.plan.commands.use.MessageBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.text.TextStringBuilder;

import java.util.Collection;

public class ForgeMessageBuilder implements MessageBuilder {

    private final CommandSourceStack sender;
    private final MutableComponent builder;
    private final ForgeMessageBuilder previous;

    public ForgeMessageBuilder(CommandSourceStack sender) {
        this(sender, null);
    }

    ForgeMessageBuilder(CommandSourceStack sender, ForgeMessageBuilder previous) {
        this.sender = sender;
        this.builder = Component.literal("");
        this.previous = previous;
    }

    @Override
    public MessageBuilder addPart(String s) {
        ForgeMessageBuilder newBuilder = new ForgeMessageBuilder(sender, this);
        newBuilder.builder.append(Component.literal(s));
        return newBuilder;
    }

    @Override
    public MessageBuilder newLine() {
        builder.append(Component.literal("\n"));
        return this;
    }

    @Override
    public MessageBuilder link(String url) {
        builder.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        return this;
    }

    @Override
    public MessageBuilder command(String command) {
        builder.withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command.charAt(0) == '/' ? command : '/' + command)));
        return this;
    }

    @Override
    public MessageBuilder hover(String message) {
        builder.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(message))));
        return this;
    }

    @Override
    public MessageBuilder hover(String... lines) {
        builder.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(new TextStringBuilder().appendWithSeparators(lines, "\n").toString()))));
        return this;
    }

    @Override
    public MessageBuilder hover(Collection<String> lines) {
        builder.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(new TextStringBuilder().appendWithSeparators(lines, "\n").toString()))));
        return this;
    }

    @Override
    public MessageBuilder indent(int amount) {
        for (int i = 0; i < amount; i++) {
            builder.append(Component.literal(" "));
        }
        return this;
    }

    @Override
    public MessageBuilder tabular(CharSequence charSequence) {
        addPart(((CMDSender) sender).getFormatter().table(charSequence.toString(), ":"));
        return this;
    }

    @Override
    public void send() {
        if (previous == null) {
            sender.sendSuccess(builder, false);
        } else {
            previous.builder.append(builder);
            previous.send();
        }
    }
}
