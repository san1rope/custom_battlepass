package mc.custombattlepass.org.custombp.database;

import mc.custombattlepass.org.custombp.CustomBP;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DatabaseH2 {
    private static Connection dbConnection;
    private Logger log = Logger.getLogger("Minecraft");

    public Connection getDbConnection() throws ClassNotFoundException, SQLException {
        String url = "jdbc:h2:" + CustomBP.getInstance().getDataFolder().getAbsolutePath() + "/userdata";
        Class.forName("org.h2.Driver");
        dbConnection = DriverManager.getConnection(url);
        return dbConnection;
    }

    public void createTableUsers() {
        String query = "CREATE TABLE IF NOT EXISTS users (nickname VARCHAR(255) NOT NULL, playeruuid VARCHAR(255) NOT NULL)";
        try {
            PreparedStatement prSt = getDbConnection().prepareStatement(query);
            prSt.executeUpdate();

            prSt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createTableProgress() {
        String query = "CREATE TABLE IF NOT EXISTS progress (nickname VARCHAR(255) NOT NULL, playeruuid VARCHAR(255) NOT NULL)";
        try {
            PreparedStatement prSt = getDbConnection().prepareStatement(query);
            prSt.executeUpdate();

            prSt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTasks(FileConfiguration config) {
        try {
            if (config.getInt("active_week") != config.getInt("temp_week")) {
                String query_show_columns = "SHOW COLUMNS FROM USERS";
                PreparedStatement prSt = getDbConnection().prepareStatement(query_show_columns);
                ResultSet resultSetColumns = prSt.executeQuery();
                List<String> columns = new ArrayList<>();
                while (resultSetColumns.next()) columns.add(resultSetColumns.getString(1));
                columns.remove("NICKNAME");
                columns.remove("PLAYERUUID");
                prSt.close();

                if (!columns.isEmpty()) {
                    String query_drop_columns = "ALTER TABLE %table DROP COLUMN ";
                    for (String column : columns) query_drop_columns += (column + ", ");
                    log.info(ChatColor.YELLOW + "[CustomBP] Query drop columns: " + query_drop_columns);
                    log.info(ChatColor.YELLOW + "[CustomBP] Formatted query drop colymns: " + query_drop_columns.substring(0, query_drop_columns.length() - 2).replace("%table", "USERS"));

                    prSt = getDbConnection().prepareStatement(query_drop_columns.substring(0, query_drop_columns.length() - 2).replace("%table", "USERS"));
                    prSt.executeUpdate();
                    prSt.close();

                    prSt = getDbConnection().prepareStatement(query_drop_columns.substring(0, query_drop_columns.length() - 2).replace("%table", "PROGRESS"));
                    prSt.executeUpdate();
                    prSt.close();
                }

                config.set("temp_week", config.getInt("active_week"));
            }

            int week = config.getInt("active_week");
            ConfigurationSection section = config.getConfigurationSection("tasks.week_" + week);
            String query_check_columns = "ALTER TABLE %table ADD COLUMN IF NOT EXISTS %task INTEGER DEFAULT 0";
            for (String task : section.getKeys(false)) {
                PreparedStatement prSt = getDbConnection().prepareStatement(query_check_columns.replace("%table", "USERS").replace("%task", task));
                prSt.executeUpdate();
                prSt.close();

                prSt = getDbConnection().prepareStatement(query_check_columns.replace("%table", "PROGRESS").replace("%task", task));
                prSt.executeUpdate();
                prSt.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ResultSet getUser(User user, String table) {
        ResultSet resultSet = null;

        String query = "SELECT * FROM %table WHERE PLAYERUUID=?".replace("%table", table);
        try {
            PreparedStatement prSt = getDbConnection().prepareStatement(query);
            prSt.setString(1, user.getPlayeruuid());

            resultSet = prSt.executeQuery();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultSet;
    }

    public void insertUser(User user, String table) {
        String query = "INSERT INTO %table (nickname, playeruuid) VALUES (?, ?)".replace("%table", table);
        try {
            PreparedStatement prSt = getDbConnection().prepareStatement(query);
            prSt.setString(1, user.getNickname());
            prSt.setString(2, user.getPlayeruuid());
            prSt.executeUpdate();

            prSt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void makeProgress(User user, String table, String task, Integer progress) {
        String query = "UPDATE %table SET %task=%task+%progress WHERE PLAYERUUID=?".replace("%table", table).replaceAll("%task", task).replace("%progress", progress.toString());
        try {
            PreparedStatement prSt = getDbConnection().prepareStatement(query);
            prSt.setString(1, user.getPlayeruuid());
            prSt.executeUpdate();

            prSt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteUserProgress(User user, String table) {
        String query = "DELETE FROM %table WHERE PLAYERUUID=?".replace("%table", table);
        try {
            PreparedStatement prSt = getDbConnection().prepareStatement(query);
            prSt.setString(1, user.getPlayeruuid());
            prSt.executeUpdate();

            prSt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteAllUsersProgress(String table, FileConfiguration config) {
        int week = config.getInt("active_week");
        ConfigurationSection section = config.getConfigurationSection("tasks.week_" + week);
        String query = "UPDATE %table SET ";
        for (String task : section.getKeys(false)) query += task.toUpperCase() + "=0, ";
        try {
            PreparedStatement prSt = getDbConnection().prepareStatement(query.substring(0, query.length() - 2).replace("%table", table));
            prSt.executeUpdate();
            prSt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
