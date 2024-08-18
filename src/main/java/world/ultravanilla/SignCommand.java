package world.ultravanilla;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SignCommand  implements TabExecutor, Listener {
    public ChatColor color = ChatColor.WHITE;
    public static final ChatColor COLOR = ChatColor.of("#d1bcad");
    public static final ChatColor WRONG_COLOR = ChatColor.of("#ce7b91");
    public static final ChatColor RIGHT_COLOR = ChatColor.of("#7bcec6");

    public static final String SPLIT_LINES = "[^\\\\]\\|";

    public SignCommand(Signage instance) {
        this.color = COLOR;
    }

    public static boolean canColor(Player player) {
        return player.hasPermission("ultravanilla.sign.color");
    }

    public static boolean isValid(String arg) {
        int size = arg.split(SPLIT_LINES).length;
        return size > 0 && size <= 4;
    }

    public static void rewriteSign(Block block, Sign sign, Player player, String arg) {
        String[] lines = new String[4];
        String[] gotLines = arg.replaceAll("\\\\\\|", "\n").split("\\|");
        for (int i = 0; i < lines.length; i++) {
            if (i < gotLines.length) {
                String line = gotLines[i].trim().replaceAll("\n", "|");
                if (canColor(player)) {
                    line = Palette.translate(line);
                }
                sign.setLine(i, line);
            } else {
                sign.setLine(i, "");
            }
        }
        fakeBreakEvent(block, player);
        sign.update();
        fakePlaceEvent(block, player);
    }

    public static void fakePlaceEvent(Block block, Player player) {
        BlockState blockState = block.getState();
        new BlockPlaceEvent(block, blockState, block, player.getEquipment().getItemInMainHand(), player, true, EquipmentSlot.HAND)
            .callEvent();
    }
    public static void fakeBreakEvent(Block block, Player player) {
        new BlockBreakEvent(block, player)
            .callEvent();
    }
    
    /** Workaround for converting legacy color strings to {@link TextColor} */
    public static TextColor legacyToTextColor(String legacy) {
        Color lColor = LegacyColors.getColor(legacy);
        return TextColor.color(
            lColor.getRed(),
            lColor.getGreen(),
            lColor.getBlue()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent e) {
        Player player = e.getPlayer();
        if (!canColor(player)) return;

        List<Component> lines = e.lines();

        for (int i = 0; i < lines.size(); i++) {
            Component oldLine = lines.get(i);
            Component newLine = oldLine.color(null);
            e.line(i, newLine);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            Block block = player.getTargetBlockExact(3);
            Sign sign;
            if (block != null && block.getState() instanceof Sign) {
                sign = (Sign) block.getState();
            } else {
                sender.sendMessage(COLOR + "You must be looking at a " + WRONG_COLOR + "sign" + COLOR + " to execute this command!");
                return true;
            }
            if (args.length == 1) {
                String arg1 = args[0];
                if (arg1.equals("clear")) {
                    for (int i = 0; i < sign.getSide(Side.FRONT).getLines().length; i++) {
                        sign.getSide(Side.FRONT).setLine(i, "");
                    }
                    for (int i = 0; i < sign.getSide(Side.BACK).getLines().length; i++) {
                        sign.getSide(Side.BACK).setLine(i, "");
                    }
                    fakeBreakEvent(block, player);
                    sign.update();
                    fakePlaceEvent(block, player);
                    sender.sendMessage(RIGHT_COLOR + "Cleared" + COLOR + " this sign.");
                    return true;
                }
            } else if (args.length == 2) {
                String arg1 = args[0];
                String arg2 = args[1];
                if (arg1.equals("color")) {
                    if (canColor(player)) {
                        try {
                            ChatColor color = ChatColor.of(arg2);
                            for (int i = 0; i < 4; i++) {
                                String line = sign.getSide(Side.FRONT).getLine(i);
                                if (!line.isEmpty()) {
                                    sign.getSide(Side.FRONT).setLine(i, color + ChatColor.stripColor(line));
                                }
                            }
                            for (int i = 0; i < 4; i++) {
                                String line = sign.getSide(Side.BACK).getLine(i);
                                if (!line.isEmpty()) {
                                    sign.getSide(Side.BACK).setLine(i, color + ChatColor.stripColor(line));
                                }
                            }
                            fakeBreakEvent(block, player);
                            sign.update();
                            fakePlaceEvent(block, player);
                            sender.sendMessage(RIGHT_COLOR + "Colored" + COLOR + " this sign " + color + arg2 + COLOR + ".");
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(WRONG_COLOR + arg2 + COLOR + " is not a valid color.");
                        }
                    } else {
                        sender.sendMessage(Signage.getInstance().getString("no-permission", "{action}", "color a sign via command."));
                    }
                    return true;
                } else if (arg1.equals("rewrite")) {
                    if (isValid(arg2)) {
                        rewriteSign(block, sign, player, arg2);
                        sender.sendMessage(RIGHT_COLOR + "Successfully " + COLOR + "rewrote this sign!");
                    } else {
                        sender.sendMessage(WRONG_COLOR + "Invalid number of lines.");
                    }
                    return true;
                }
            } else {
                if (args.length > 2) {
                    String arg1 = args[0];
                    String arg;
                    if (arg1.equals("rewrite")) {
                        arg = getArg(args, 2);
                        if (isValid(arg)) {
                            rewriteSign(block, sign, player, arg);
                            sender.sendMessage(RIGHT_COLOR + "Successfully " + COLOR + "rewrote this sign!");
                        } else {
                            sender.sendMessage(WRONG_COLOR + "Invalid number of lines.");
                        }
                        return true;
                    } else if (arg1.equals("setline")) {
                        String arg2 = args[1];
                        arg = getArg(args, 3);
                        if (canColor(player)) {
                            arg = Palette.translate(arg);
                        }
                        try {
                            int lineNumber = Integer.parseInt(arg2);
                            if (lineNumber > 0 && lineNumber <= 4) {
                                String lastLine = sign.getSide(Side.FRONT).getLine(lineNumber - 1);
                                sign.getSide(Side.FRONT).setLine(lineNumber - 1, arg);
                                fakeBreakEvent(block, player);
                                sign.update();
                                fakePlaceEvent(block, player);
                                if (lastLine.isEmpty()) {
                                    sender.sendMessage(String.format("%sSet %sline %d %sto %s%s", COLOR, RIGHT_COLOR, lineNumber, COLOR, ChatColor.RESET, arg));
                                } else {
                                    sender.sendMessage(String.format("%sChanged %sline %d %sfrom %s%s %sto %s%s", COLOR, RIGHT_COLOR, lineNumber, COLOR, ChatColor.RESET, lastLine, COLOR, ChatColor.RESET, arg));
                                }
                            
                            } if (lineNumber > 5 && lineNumber <= 8) {
                                String lastLine = sign.getSide(Side.BACK).getLine(lineNumber - 5);
                                sign.getSide(Side.BACK).setLine(lineNumber - 5, arg);
                                fakeBreakEvent(block, player);
                                sign.update();
                                fakePlaceEvent(block, player);
                                if (lastLine.isEmpty()) {
                                    sender.sendMessage(String.format("%sSet %sline %d %sto %s%s", COLOR, RIGHT_COLOR, lineNumber, COLOR, ChatColor.RESET, arg));
                                } else {
                                    sender.sendMessage(String.format("%sChanged %sline %d %sfrom %s%s %sto %s%s", COLOR, RIGHT_COLOR, lineNumber, COLOR, ChatColor.RESET, lastLine, COLOR, ChatColor.RESET, arg));
                                }
                            } else {
                                sender.sendMessage(String.format("%s%s %sneeds to be a number %sbetween 1 and 8", WRONG_COLOR, arg, COLOR, RIGHT_COLOR));
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage(String.format("%s%s %sis not a valid number.", WRONG_COLOR, arg2, COLOR));
                        }
                        return true;
                    } else if (arg1.equals("colorline")) {
                        if (canColor(player)) {
                            String arg2 = args[1];
                            String arg3 = args[2];
                            try {
                                ChatColor color = ChatColor.of(arg3);
                                int lineNumber = Integer.parseInt(arg2);
                                if (lineNumber > 0 && lineNumber <= 4) {
                                    sign.getSide(Side.FRONT).setLine(lineNumber - 1, color + ChatColor.stripColor(sign.getLine(lineNumber - 1)));
                                    fakeBreakEvent(block, player);
                                    sign.update();
                                    fakePlaceEvent(block, player);
                                    sender.sendMessage(RIGHT_COLOR + "Colored" + COLOR + " line " + RIGHT_COLOR + lineNumber + " " + color + arg3 + COLOR + ".");
                                } if (lineNumber > 5 && lineNumber <= 8) {
                                    sign.getSide(Side.BACK).setLine(lineNumber - 5, color + ChatColor.stripColor(sign.getLine(lineNumber - 1)));
                                    fakeBreakEvent(block, player);
                                    sign.update();
                                    fakePlaceEvent(block, player);
                                    sender.sendMessage(RIGHT_COLOR + "Colored" + COLOR + " line " + RIGHT_COLOR + lineNumber + " " + color + arg3 + COLOR + ".");
                                } else {
                                    sender.sendMessage(WRONG_COLOR + arg2 + COLOR + " needs to be a number " + RIGHT_COLOR + "between 1 and 8");
                                }
                            } catch (NumberFormatException e) {
                                sender.sendMessage(WRONG_COLOR + arg2 + COLOR + " is not a valid number.");
                            } catch (IllegalArgumentException e2) {
                                sender.sendMessage(WRONG_COLOR + arg3 + COLOR + " is not a valid color.");
                            }
                        } else {
                            sender.sendMessage(Signage.getInstance().getString("no-permission", "{action}", "color a sign via command."));
                        }
                    }
                    return true;
                }
            }
            return false;
        } else {
            sender.sendMessage(COLOR + "You must be a player to use " + WRONG_COLOR + "/sign" + COLOR + ".");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (sender instanceof Player) {
            boolean canColor = canColor((Player) sender);
            if (args.length == 1) {
                suggestions.add("edit");
                suggestions.add("rewrite");
                suggestions.add("setline");
                if (canColor) {
                    suggestions.add("color");
                    suggestions.add("colorline");
                }
                suggestions.add("clear");
                suggestions.add("open");
            } else if (args.length == 2) {
                String arg1 = args[0];
                if (arg1.matches("setline|colorline")) {
                    suggestions.add("1");
                    suggestions.add("2");
                    suggestions.add("3");
                    suggestions.add("4");
                    suggestions.add("5");
                    suggestions.add("6");
                    suggestions.add("7");
                    suggestions.add("8");
                } else if (canColor && arg1.matches("color")) {
                    suggestions.addAll(Arrays.asList(LegacyColors.listNames()));
                }
            } else if (args.length == 3) {
                String arg1 = args[0];
                if (canColor && arg1.matches("colorline")) {
                    suggestions.addAll(Arrays.asList(LegacyColors.listNames()));
                } else if (arg1.matches("setline")) {
                    Block block = ((Player) sender).getTargetBlockExact(3);
                    if (block != null && block.getState() instanceof Sign) {
                        int signLineNumber = Integer.parseInt(args[1]);
                        String line;
                        if (signLineNumber < 5) {
                            line = ((Sign) block.getState()).getSide(Side.FRONT).getLine(signLineNumber - 1);
                        } else {
                            line = ((Sign) block.getState()).getSide(Side.BACK).getLine(signLineNumber - 5);
                        }
                        suggestions.add(Palette.untranslate(line));
                    }
                }
            }
        }
        List<String> realSuggestions = new ArrayList<>();
        for (String s : suggestions) {
            if (args[args.length - 1].length() < args.length || s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                realSuggestions.add(s);
            }
        }
        return realSuggestions;
    }
    
    protected String getArg(String[] args, int index) {
        StringBuilder message = new StringBuilder();
        for (int i = index - 1; i < args.length; i++) {
            message.append(args[i]).append(" ");
        }
        return message.toString().trim();
    }
}
