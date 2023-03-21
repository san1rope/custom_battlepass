package mc.custombattlepass.org.custombp.util;

import mc.custombattlepass.org.custombp.CustomBP;
import mc.custombattlepass.org.custombp.database.DatabaseH2;
import mc.custombattlepass.org.custombp.database.User;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiscFunc {
    public final Pattern HEX_PATTERN = Pattern.compile("&#([a-f0-9]{6})");
    private static Logger log = Logger.getLogger("Minecraft");
    private static FileConfiguration config = CustomBP.getInstance().getConfig();

    public String hexToString(String message) {
        Matcher matcher = this.HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
                    + ChatColor.COLOR_CHAR + group.charAt(0) + ChatColor.COLOR_CHAR + group.charAt(1)
                    + ChatColor.COLOR_CHAR + group.charAt(2) + ChatColor.COLOR_CHAR + group.charAt(3)
                    + ChatColor.COLOR_CHAR + group.charAt(4) + ChatColor.COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }

    public static String getCurrentTask(Player player) throws SQLException {
        DatabaseH2 dbHandler = new DatabaseH2();
        User user = new User(player.getName(), player.getUniqueId().toString());
        int active_week = config.getInt("active_week");
        ConfigurationSection section = config.getConfigurationSection("tasks.week_" + active_week);
        for (String n : section.getKeys(false)) {
            ResultSet resultSet = dbHandler.getUser(user, "USERS");
            while (resultSet.next()) {
                Integer temp = resultSet.getInt(n.toUpperCase());
                if (temp == 0) return (n.toUpperCase());
            }
        }
        return null;
    }
}
