package dev.nucleusreel;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import dev.nucleusreel.core.Reward;
import dev.nucleusreel.core.RewardCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

public final class RewardIcons {
    private RewardIcons() {}

    public static ItemStack forReward(Reward reward) {
        var definition = RewardCatalog.find(reward.name());
        if (definition == null) {
            ItemStack unknown = new ItemStack(Items.PAPER);
            unknown.set(DataComponents.CUSTOM_NAME, Component.literal(reward.name()));
            return unknown;
        }
        return create(definition, model -> Minecraft.getInstance().getResourceManager().getResource(
            Identifier.fromNamespaceAndPath(model.getNamespace(), "items/" + model.getPath() + ".json")
        ).isPresent());
    }

    /** Does not require an inventory or a player; caller supplies pack availability. */
    public static ItemStack create(RewardCatalog.Definition definition, Predicate<Identifier> modelAvailable) {
        var material = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(definition.material()));
        ItemStack stack = new ItemStack(material);
        CompoundTag data = new CompoundTag();
        data.putString("id", definition.id());
        if (definition.enchantment() != null) {
            CompoundTag enchantments = new CompoundTag();
            enchantments.putInt(definition.enchantment(), definition.enchantmentLevel());
            data.put("enchantments", enchantments);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(definition.name())
            .withStyle(rarityColor(definition.tier())).withStyle(style -> style.withItalic(false)));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, definition.glowing());
        if (definition.skinValue() != null && !definition.skinValue().isBlank()) {
            Property property = definition.skinSignature() == null
                ? new Property("textures", definition.skinValue())
                : new Property("textures", definition.skinValue(), definition.skinSignature());
            UUID uuid = UUID.nameUUIDFromBytes(definition.skinValue().getBytes(StandardCharsets.UTF_8));
            GameProfile profile = new GameProfile(uuid, "NucleusReel", new PropertyMap(ImmutableMultimap.of("textures", property)));
            stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        }
        // Catharsis applies SkyBlock-ID models using the user's pack configuration.
        if (definition.itemModel() != null) {
            Identifier model = Identifier.parse(definition.itemModel());
            if (modelAvailable.test(model)) stack.set(DataComponents.ITEM_MODEL, model);
        }
        return stack;
    }

    private static ChatFormatting rarityColor(String tier) {
        if (tier == null) return ChatFormatting.WHITE;
        return switch (tier) {
            case "UNCOMMON" -> ChatFormatting.GREEN;
            case "RARE" -> ChatFormatting.BLUE;
            case "EPIC" -> ChatFormatting.DARK_PURPLE;
            case "LEGENDARY" -> ChatFormatting.GOLD;
            case "MYTHIC" -> ChatFormatting.LIGHT_PURPLE;
            case "SPECIAL", "VERY_SPECIAL" -> ChatFormatting.RED;
            default -> ChatFormatting.WHITE;
        };
    }

    public static int color(Reward reward) {
        return dev.nucleusreel.core.RewardColor.of(reward);
    }

    public static boolean isJackpot(Reward reward) {
        String name = RewardCatalog.normalize(reward.name());
        return name.equals("divan's alloy") || name.equals("quick claw");
    }
}
