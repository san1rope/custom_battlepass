package mc.custombattlepass.org.custombp;

import mc.custombattlepass.org.custombp.command.PassCommand;
import mc.custombattlepass.org.custombp.database.DatabaseH2;
import mc.custombattlepass.org.custombp.database.User;
import mc.custombattlepass.org.custombp.util.MiscFunc;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class EventListener implements Listener {
    private List<String> lore = new ArrayList<>();
    private MiscFunc mf = new MiscFunc();
    private DatabaseH2 dbHandler = new DatabaseH2();
    private FileConfiguration config = CustomBP.getInstance().getConfig();
    private Logger log = Logger.getLogger("Minecraft");

    @EventHandler
    public void onJoin(PlayerJoinEvent e) throws SQLException {
        Player p = e.getPlayer();

        User user = new User(p.getName(), p.getUniqueId().toString());
        ResultSet resultSet = dbHandler.getUser(user,"USERS");

        resultSet.last();
        byte result_count = (byte) resultSet.getRow();
        if (result_count == 0) {
            dbHandler.insertUser(user, "USERS");
            dbHandler.insertUser(user, "PROGRESS");
        }
        return;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) throws SQLException {
        Player p = (Player) e.getWhoClicked();

        if (e.getView().getTitle().equalsIgnoreCase(mf.hexToString(config.getString("messages.menu_title")))) {
            e.setCancelled(true);

            if (e.getSlot() == PassCommand.itemSlot.get("prev_page")) {
                int page = PassCommand.temp_page.get(p.getUniqueId());
                if (page == 1) return;
                PassCommand.openGUI(p, config, page - 1);
                PassCommand.temp_page.put(p.getUniqueId(), page - 1);
                return;
            }

            if (e.getSlot() == PassCommand.itemSlot.get("next_page")) {
                int page = PassCommand.temp_page.get(p.getUniqueId());
                PassCommand.openGUI(p, config, page + 1);
                PassCommand.temp_page.put(p.getUniqueId(), page + 1);
                return;
            }

            if (e.getCurrentItem() == null) return;
            String displayName = e.getCurrentItem().getItemMeta().getDisplayName();
            ConfigurationSection section = null;
            int week = config.getInt("active_week");
            if (displayName.startsWith("Задание")) {
                String[] ex = displayName.split("\\D+");

                // Getting lore
                lore = (List<String>) config.getList("tasks.week_" + week + ".ex_" + ex[1] + ".pre_view.lore");
                for (String i : lore) p.sendMessage(i);

                e.getInventory().close();
                return;
            }
        }
        return;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) throws SQLException {
        Player p = e.getPlayer();
        Material block_type = e.getBlock().getType();
        Bukkit.getScheduler().runTaskAsynchronously(CustomBP.getInstance(), () -> {
            String task = null;
            try {
                task = mf.getCurrentTask(p);
                if (task == null) return;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            int week = config.getInt("active_week");
            ConfigurationSection section = config.getConfigurationSection("tasks.week_" + week + "." + task.toLowerCase());
            List<String> commands = new ArrayList<>();

            if (!section.getString("action").trim().equalsIgnoreCase("break_block")) return;
            if (block_type != Material.getMaterial(section.getString("action_type"))) return;

            User user = new User(p.getName(), p.getUniqueId().toString());
            dbHandler.makeProgress(user, "PROGRESS", task, 1);

            ResultSet resultSet = dbHandler.getUser(user, "PROGRESS");
            int progress = 0;
            try {
                while (resultSet.next()) {
                    progress = resultSet.getInt(task);
                    if (progress >= section.getInt("counter")) dbHandler.makeProgress(user, "USERS", task, 1);
                    else return;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            commands = (List<String>) section.getList("dispatch_after_exec");
            p.sendMessage(PlaceholderAPI.setPlaceholders(p, PlaceholderAPI.setPlaceholders(p, mf.hexToString(config.getString("messages.prefix") + config.getString("messages.task_completed_message")))));
            for (String cmd : commands) {
                if (cmd.startsWith("[console]")) {
                    String cmd_fil = cmd.replace("[console]", "");
                    String final_cmd_fil = PlaceholderAPI.setPlaceholders(p, cmd_fil.trim());
                    Bukkit.getScheduler().callSyncMethod(CustomBP.getInstance(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), final_cmd_fil));
                    continue;
                }

                if (cmd.startsWith("[player]")) {
                    String cmd_fil = cmd.replace("[player]", "");
                    String final_cmd_fil = PlaceholderAPI.setPlaceholders(p, cmd_fil.trim());
                    Bukkit.getScheduler().callSyncMethod(CustomBP.getInstance(), () -> p.performCommand(final_cmd_fil));
                    continue;
                }

                log.warning(ChatColor.RED + "[CustomBP] Вы не указали отправителя в dispatch_after_exec");
                log.warning(ChatColor.RED + "[CustomBP] Week: " + week);
                log.warning(ChatColor.RED + "[CustomBP] Task: " + task);
            }
        });

        return;
    }

    @EventHandler
    public void onPickUpItem(PlayerPickupItemEvent e) throws SQLException {
        Player p = e.getPlayer();
        Material item_type = e.getItem().getItemStack().getType();
        Bukkit.getScheduler().runTaskAsynchronously(CustomBP.getInstance(), () -> {
            String task = null;
            try {
                task = mf.getCurrentTask(p);
                if (task == null) return;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            int week = config.getInt("active_week");
            ConfigurationSection section = config.getConfigurationSection("tasks.week_" + week + "." + task.toLowerCase());
            List<String> commands = new ArrayList<>();

            if (!section.getString("action").trim().equalsIgnoreCase("pickup_item")) return;
            if (item_type != Material.getMaterial(section.getString("action_type"))) return;

            int amount = e.getItem().getItemStack().getAmount();
            User user = new User(p.getName(), p.getUniqueId().toString());
            dbHandler.makeProgress(user, "PROGRESS", task, amount);

            ResultSet resultSet = dbHandler.getUser(user, "PROGRESS");
            int progress = 0;
            try {
                while (resultSet.next()) {
                    progress = resultSet.getInt(task);
                    if (progress >= section.getInt("counter")) dbHandler.makeProgress(user, "USERS", task, 1);
                    else return;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            commands = (List<String>) section.getList("dispatch_after_exec");
            p.sendMessage(PlaceholderAPI.setPlaceholders(p, PlaceholderAPI.setPlaceholders(p, mf.hexToString(config.getString("messages.prefix") + config.getString("messages.task_completed_message")))));
            for (String cmd : commands) {
                if (cmd.startsWith("[console]")) {
                    String cmd_fil = cmd.replace("[console]", "");
                    String final_cmd_fil = PlaceholderAPI.setPlaceholders(p, cmd_fil.trim());
                    Bukkit.getScheduler().callSyncMethod(CustomBP.getInstance(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), final_cmd_fil));
                    continue;
                }

                if (cmd.startsWith("[player]")) {
                    String cmd_fil = cmd.replace("[player]", "");
                    String final_cmd_fil = PlaceholderAPI.setPlaceholders(p, cmd_fil.trim());
                    Bukkit.getScheduler().callSyncMethod(CustomBP.getInstance(), () -> p.performCommand(final_cmd_fil));
                    continue;
                }

                log.warning("[CustomBP] Вы не указали отправителя в dispatch_after_exec");
                log.warning("[CustomBP] Week: " + week);
                log.warning("[CustomBP] Task: " + task);
            }
        });
    }

    @EventHandler
    public void onLivingDeath(EntityDeathEvent e) throws SQLException {
        Player p = e.getEntity().getKiller();
        if (p == null) return;
        EntityType entity = e.getEntity().getType();
        Bukkit.getScheduler().runTaskAsynchronously(CustomBP.getInstance(), () -> {
            String task = null;
            try {
                task = mf.getCurrentTask(p);
                if (task == null) return;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            int week = config.getInt("active_week");
            ConfigurationSection section = config.getConfigurationSection("tasks.week_" + week + "." + task.toLowerCase());
            List<String> commands = new ArrayList<>();

            if (!section.getString("action").trim().equalsIgnoreCase("kill_entity")) return;
            if (entity != EntityType.fromName(section.getString("action_type"))) return;

            User user = new User(p.getName(), p.getUniqueId().toString());
            dbHandler.makeProgress(user, "PROGRESS", task, 1);

            ResultSet resultSet = dbHandler.getUser(user, "PROGRESS");
            int progress = 0;
            try {
                while (resultSet.next()) {
                    progress = resultSet.getInt(task);
                    if (progress >= section.getInt("counter")) dbHandler.makeProgress(user, "USERS", task, 1);
                    else return;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            commands = (List<String>) section.getList("dispatch_after_exec");
            p.sendMessage(PlaceholderAPI.setPlaceholders(p, PlaceholderAPI.setPlaceholders(p, mf.hexToString(config.getString("messages.prefix") + config.getString("messages.task_completed_message")))));
            for (String cmd : commands) {
                if (cmd.startsWith("[console]")) {
                    String cmd_fil = cmd.replace("[console]", "");
                    String final_cmd_fil = PlaceholderAPI.setPlaceholders(p, cmd_fil.trim());
                    Bukkit.getScheduler().callSyncMethod(CustomBP.getInstance(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), final_cmd_fil));
                    continue;
                }

                if (cmd.startsWith("[player]")) {
                    String cmd_fil = cmd.replace("[player]", "");
                    String final_cmd_fil = PlaceholderAPI.setPlaceholders(p, cmd_fil.trim());
                    Bukkit.getScheduler().callSyncMethod(CustomBP.getInstance(), () -> p.performCommand(final_cmd_fil));
                    continue;
                }

                log.warning("[CustomBP] Вы не указали отправителя в dispatch_after_exec");
                log.warning("[CustomBP] Week: " + week);
                log.warning("[CustomBP] Task: " + task);
            }
        });
    }
}
