package mc.custombattlepass.org.custombp.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class ItemUtil {
    private static MiscFunc mf = new MiscFunc();

    public static ItemStack create(Material material, Integer amount, byte data, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material, amount, data);
        ItemMeta meta = item.getItemMeta();

        if (displayName != null) {
            meta.setDisplayName(displayName);
        }

        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack create(Material material, String displayName) {
        return create(material, 1, (byte) 0, displayName, null);
    }

    public static ItemStack create(Material material, Integer amount, byte data, String displayName) {
        return create(material, amount, data, displayName, null);
    }

    public static ItemStack create(Material material, String displayName, String lore1, String lore2, String lore3, String lore4) {
        return create(material, 1, (byte) 0, displayName, null);
    }

    @NonNull
    public static ItemStack getHeadByTextureData(@NonNull String value, boolean base64, String displayName, List<String> lore, Player player) {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
        if (!base64) {
            value = new String(Base64.getEncoder().encode(value.getBytes()));
        }
        profile.setProperty(new ProfileProperty("textures", value));
        return createPlayerHeadWithProfile(profile, displayName, lore, player);
    }

    @NonNull
    private static ItemStack createPlayerHeadWithProfile(@Nullable PlayerProfile profile, String displayName, List<String> lore, Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (profile != null) {
            SkullMeta headMeta = (SkullMeta) head.getItemMeta();
            headMeta.setPlayerProfile(profile);
            headMeta.setDisplayName(displayName);

            List<String> formatted_lore = new ArrayList<>();
            if (lore != null) {
                for (String i : lore) formatted_lore.add(PlaceholderAPI.setPlaceholders(player, mf.hexToString(i)));
                headMeta.setLore(formatted_lore);
            }
            head.setItemMeta(headMeta);
        }
        return head;
    }
}
