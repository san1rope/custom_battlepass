package mc.custombattlepass.org.custombp;

import mc.custombattlepass.org.custombp.database.DatabaseH2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class TimeTracking {
    private static ConfigurationSection section = CustomBP.getInstance().getConfig().getConfigurationSection("starting_point");
    private static Logger log = Logger.getLogger("Minecraft");

    public static void setStartPoint() {
        if (section.getString("year") == null || section.getString("month") == null ||
                section.getString("day") == null || section.getString("hour") == null ||
                section.getString("minute") == null || section.getString("second") == null) {
            LocalDateTime dateTime = LocalDateTime.now();

            section.set("year", dateTime.getYear());
            section.set("month", dateTime.getMonthValue());
            section.set("day", dateTime.getDayOfMonth());
            section.set("hour", dateTime.getHour());
            section.set("minute", dateTime.getMinute());
            section.set("second", dateTime.getSecond());
        }
    }

    public static void startTimeTracking() {
        log.info(ChatColor.GREEN + "[CustomBP] Time Tracking start!");
        while (true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            LocalDateTime dateTimeNow = LocalDateTime.now();
            LocalDateTime dateTimeEndPoint = LocalDateTime.of(section.getInt("year"),
                    section.getInt("month"), section.getInt("day"), section.getInt("hour"),
                    section.getInt("minute"), section.getInt("second")).plusWeeks(1);

            if (dateTimeNow.isAfter(dateTimeEndPoint)) {
                FileConfiguration config = CustomBP.getInstance().getConfig();

                int week = config.getInt("active_week");
                config.set("active_week", week + 1);
                log.info(ChatColor.GREEN + "[CustomBP] It's been 7 days, the beginning of a new week...");

                section.set("year", dateTimeNow.getYear());
                section.set("month", dateTimeNow.getMonthValue());
                section.set("day", dateTimeNow.getDayOfMonth());
                section.set("hour", dateTimeNow.getHour());
                section.set("minute", dateTimeNow.getMinute());
                section.set("second", dateTimeNow.getSecond());

                DatabaseH2 dbHandler = new DatabaseH2();
                dbHandler.updateTasks(config);
            }
        }
    }
}
