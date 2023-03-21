package mc.custombattlepass.org.custombp;

import mc.custombattlepass.org.custombp.command.PassCommand;
import mc.custombattlepass.org.custombp.database.DatabaseH2;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class CustomBP extends JavaPlugin {

    private static CustomBP instance;
    DatabaseH2 dbHandler = new DatabaseH2();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        TimeTracking.setStartPoint();

        dbHandler.createTableUsers();
        dbHandler.createTableProgress();
        dbHandler.updateTasks(getConfig());

        new PassCommand();
        Bukkit.getPluginManager().registerEvents(new EventListener(), this);

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> TimeTracking.startTimeTracking());
    }

    @Override
    public void onDisable() {
        saveConfig();
        getServer().getScheduler().cancelTasks(this);
    }

    public static CustomBP getInstance() {
        return instance;
    }
}
