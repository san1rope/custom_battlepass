package mc.custombattlepass.org.custombp.command;

import com.google.common.collect.Lists;
import mc.custombattlepass.org.custombp.CustomBP;
import mc.custombattlepass.org.custombp.database.DatabaseH2;
import mc.custombattlepass.org.custombp.database.User;
import mc.custombattlepass.org.custombp.util.ItemUtil;
import mc.custombattlepass.org.custombp.util.MiscFunc;
import me.clip.placeholderapi.PlaceholderAPI;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class PassCommand extends AbstractCommand {
    public PassCommand() {
        super("pass");
    }

    private static MiscFunc mf = new MiscFunc();
    public static HashMap<UUID, Integer> temp_page = new HashMap<>();
    private DatabaseH2 dbHandler = new DatabaseH2();
    public static HashMap<String, Integer> itemSlot = new HashMap<>();
    private static Logger log = Logger.getLogger("Minecraft");

    public static void openGUI(Player player, FileConfiguration config, Integer page) throws SQLException {
        Inventory inv = Bukkit.createInventory(null, 54, mf.hexToString(config.getString("messages.menu_title")));
        List<String> lore = new ArrayList<>();
        String head_open_chest = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjZhY2ZjNjQzZjYwOGUxNmRlMTkzMzVkZGNhNzFhODI4ZGZiOGRhY2E1NzkzZWI1YmJjYjBjN2QxNTU5MjQ5In19fQ==";
        String head_closed_chest = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWRjMzZjOWNiNTBhNTI3YWE1NTYwN2EwZGY3MTg1YWQyMGFhYmFhOTAzZThkOWFiZmM3ODI2MDcwNTU0MGRlZiJ9fX0=";

        for (int i = 0; i <= 53; i++) {
            if (i == 9) i = 27;
            if (i == 36) i = 45;
            if (i == 45) {
                String arrow_owner = config.getString("item_prev_page");
                ItemStack item = null;
                String displayName = PlaceholderAPI.setPlaceholders(player, mf.hexToString(config.getString("messages.prev_page")));
                if (arrow_owner.startsWith("head:")) {
                    item = ItemUtil.getHeadByTextureData(arrow_owner.substring(5), true, displayName, null, player);
                } else {
                    item = ItemUtil.create(Material.getMaterial(arrow_owner), displayName);
                }
                itemSlot.put("prev_page", i);
                inv.setItem(i, item);
                continue;
            }
            if (i == 53) {
                String arrow_owner = config.getString("item_next_page");
                ItemStack item = null;
                String displayName = PlaceholderAPI.setPlaceholders(player, mf.hexToString(config.getString("messages.next_page")));
                if (arrow_owner.startsWith("head:")) {
                    item = ItemUtil.getHeadByTextureData(arrow_owner.substring(5), true, displayName, null, player);
                } else {
                    item = ItemUtil.create(Material.getMaterial(arrow_owner), mf.hexToString(config.getString("messages.next_page")));
                }
                itemSlot.put("next_page", i);
                inv.setItem(i, item);
                continue;
            }
            inv.setItem(i, ItemUtil.create(Material.BLACK_STAINED_GLASS_PANE, "BattlePass"));
        }

        int i = page * 9 - 8;
        byte n = 9;
        int week = config.getInt("active_week");

        ConfigurationSection section = config.getConfigurationSection("tasks.week_" + week);

        String item = section.getString("ex_" + i + ".pre_view.item");
        if (item == null) return;

        DatabaseH2 dbHandler = new DatabaseH2();

        while (i <= page * 9) {
            section = config.getConfigurationSection("tasks.week_" + week + ".ex_" + i);
            if (section == null) break;
            item = section.getString("pre_view.item");
            if (item == null) break;
            ItemStack itemStack = null;
            lore = (List<String>) section.getList("pre_view.lore");
            if (item.startsWith("head:")) {
                String value = item.substring(5);
                itemStack = ItemUtil.getHeadByTextureData(value, true, ("Задание #" + i), lore, player);
                ItemMeta tempMeta = itemStack.getItemMeta();
                tempMeta.setLore(lore);
            } else {
                itemStack = ItemUtil.create(Material.getMaterial(item), ("Задание #" + i), lore.get(0), lore.get(1), lore.get(2), lore.get(3));
            }
            inv.setItem(n, itemStack);

            int counter = section.getInt("counter");
            User user = new User(player.getName(), player.getUniqueId().toString());
            ResultSet resultSet = dbHandler.getUser(user, "PROGRESS");
            while (resultSet.next()) {
                byte percent = (byte) (resultSet.getInt("EX_" + i) / counter);
                if (percent < 1) {
                    inv.setItem((n + 27), ItemUtil.getHeadByTextureData(head_closed_chest, true, PlaceholderAPI.setPlaceholders(player, mf.hexToString(config.getString("messages.task_not_completed"))), null, player));
                } else {
                    inv.setItem((n + 27), ItemUtil.getHeadByTextureData(head_open_chest, true, PlaceholderAPI.setPlaceholders(player, mf.hexToString(config.getString("messages.task_completed"))), null, player));
                }
            }

            i++;
            n++;
        }

        player.openInventory(inv);
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) throws SQLException {
        FileConfiguration config = CustomBP.getInstance().getConfig();

        if (!sender.hasPermission("cbattlepass.pass.user")) {
            sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.no_permission")));
            return;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.console_sender")));
                return;
            }

            UUID uuid = ((Player) sender).getUniqueId();
            temp_page.put(uuid, 1);
            openGUI((Player) sender, config, 1);
            return;
        }

        if (args[0].equalsIgnoreCase("setweek")) {
            if (!sender.hasPermission("cbattlepass.pass.setweek")) {
                sender.sendMessage(PlaceholderAPI.setPlaceholders(((Player) sender), mf.hexToString(config.getString("messages.prefix") + config.getString("messages.no_permission"))));
                return;
            }

            if (args.length < 2) {
                sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.no_args")));
                return;
            }

            if (!StringUtils.isNumeric(args[1])) {
                sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.wrong_num")));
                return;
            }

            if (config.getInt("active_week") == Integer.valueOf(args[1])) {
                sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.wrong_week")));
                return;
            }

            String week = config.getString("tasks.week_" + args[1] + ".ex_1.pre_view.item");
            if (week == null) {
                sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("wrong_week")));
                return;
            }

            config.set("active_week", Integer.valueOf(args[1]));
            ConfigurationSection section = CustomBP.getInstance().getConfig().getConfigurationSection("starting_point");
            LocalDateTime dateTime = LocalDateTime.now();

            section.set("year", dateTime.getYear());
            section.set("month", dateTime.getMonthValue());
            section.set("day", dateTime.getDayOfMonth());
            section.set("hour", dateTime.getHour());
            section.set("minute", dateTime.getMinute());
            section.set("second", dateTime.getSecond());

            dbHandler.updateTasks(config);

            sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.set_week").replace("{week}", args[1])));
            log.info(ChatColor.GREEN + "[CustomBP] {player_name} установил {week} неделю!".replace("{player_name}", sender.getName()).replace("{week}", args[1]));
            return;
        }

        if (args[0].equalsIgnoreCase("cleardata")) {
            if (args.length < 2) {
                sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.no_args")));
                return;
            }

            if (args[1].equalsIgnoreCase("*")) {
                dbHandler.deleteAllUsersProgress("USERS", config);
                dbHandler.deleteAllUsersProgress("PROGRESS", config);
                sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.delete_user_data_all")));
                return;
            }

            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.no_user_found")));
                return;
            }
            User user = new User(player.getName(), player.getUniqueId().toString());

            dbHandler.deleteUserProgress(user, "USERS");
            dbHandler.deleteUserProgress(user, "PROGRESS");

            dbHandler.insertUser(user, "USERS");
            dbHandler.insertUser(user, "PROGRESS");

            sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.delete_user_data").replace("{player_name}", args[1])));
            return;
        }

        sender.sendMessage(mf.hexToString(config.getString("messages.prefix") + config.getString("messages.usage")));
        return;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) return Lists.newArrayList("setweek", "cleardata");
        return Lists.newArrayList();
    }
}
