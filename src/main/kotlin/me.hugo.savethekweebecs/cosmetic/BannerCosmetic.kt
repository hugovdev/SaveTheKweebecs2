package me.hugo.savethekweebecs.cosmetic

import com.destroystokyo.paper.MaterialSetTag
import me.hugo.savethekweebecs.extension.*
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.util.menus.Icon
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import java.util.*

enum class BannerCosmetic(val banner: ItemStack?) {

    NONE(null),
    UNITED_KINGDOM(
        ItemStack(Material.BLUE_BANNER)
            .putPatterns(
                Pattern(DyeColor.WHITE, PatternType.STRIPE_DOWNLEFT),
                Pattern(DyeColor.WHITE, PatternType.STRIPE_DOWNRIGHT),
                Pattern(DyeColor.WHITE, PatternType.STRIPE_DOWNLEFT),
                Pattern(DyeColor.WHITE, PatternType.STRIPE_DOWNRIGHT),
                Pattern(DyeColor.RED, PatternType.CROSS),
                Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER),
                Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER),
                Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE),
                Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE),
                Pattern(DyeColor.RED, PatternType.STRAIGHT_CROSS)
            )
    ),
    SPAIN(
        ItemStack(Material.RED_BANNER).putPatterns(
            Pattern(DyeColor.YELLOW, PatternType.STRIPE_CENTER),
            Pattern(DyeColor.YELLOW, PatternType.STRIPE_CENTER)
        )
    ),
    GERMANY(
        ItemStack(Material.RED_BANNER).putPatterns(
            Pattern(DyeColor.YELLOW, PatternType.STRIPE_RIGHT),
            Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT)
        )
    ),
    COLOMBIA(
        ItemStack(Material.BLUE_BANNER).putPatterns(
            Pattern(DyeColor.YELLOW, PatternType.HALF_VERTICAL_MIRROR),
            Pattern(DyeColor.RED, PatternType.STRIPE_LEFT),
            Pattern(DyeColor.BLUE, PatternType.STRIPE_CENTER)
        )
    ),
    ARGENTINA(
        ItemStack(Material.WHITE_BANNER).putPatterns(
            Pattern(DyeColor.YELLOW, PatternType.CIRCLE_MIDDLE),
            Pattern(DyeColor.WHITE, PatternType.FLOWER),
            Pattern(DyeColor.YELLOW, PatternType.STRIPE_MIDDLE),
            Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM),
            Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP),
            Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_LEFT),
            Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_RIGHT)
        )
    ),
    FRANCE(
        ItemStack(Material.WHITE_BANNER).putPatterns(
            Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM),
            Pattern(DyeColor.BLUE, PatternType.STRIPE_TOP),
            Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM)
        )
    ),
    ITALY(
        ItemStack(Material.WHITE_BANNER).putPatterns(
            Pattern(DyeColor.GREEN, PatternType.HALF_VERTICAL),
            Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER),
            Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT)
        )
    ),
    MEXICO(
        ItemStack(Material.WHITE_BANNER).putPatterns(
            Pattern(DyeColor.GREEN, PatternType.STRIPE_TOP),
            Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM),
            Pattern(DyeColor.YELLOW, PatternType.CIRCLE_MIDDLE)
        )
    ),
    URUGUAY(
        ItemStack(Material.WHITE_BANNER).putPatterns(
            Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_SMALL),
            Pattern(DyeColor.WHITE, PatternType.SQUARE_TOP_RIGHT),
            Pattern(DyeColor.YELLOW, PatternType.SQUARE_TOP_RIGHT)
        )
    ),
    CANADA(
        ItemStack(Material.WHITE_BANNER).putPatterns(
            Pattern(DyeColor.RED, PatternType.CROSS),
            Pattern(DyeColor.RED, PatternType.STRIPE_CENTER),
            Pattern(DyeColor.WHITE, PatternType.RHOMBUS_MIDDLE),
            Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT),
            Pattern(DyeColor.RED, PatternType.CIRCLE_MIDDLE),
            Pattern(DyeColor.RED, PatternType.STRIPE_TOP),
            Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM)
        )
    ),
    USA(
        ItemStack(Material.RED_BANNER).putPatterns(
            Pattern(DyeColor.WHITE, PatternType.STRIPE_SMALL),
            Pattern(DyeColor.BLUE, PatternType.STRIPE_TOP),
            Pattern(DyeColor.BLUE, PatternType.SQUARE_TOP_LEFT),
            Pattern(DyeColor.BLUE, PatternType.SQUARE_TOP_RIGHT)
        )
    ),
    LGBT_PRIDE(
        ItemStack(Material.LIME_BANNER).putPatterns(
            Pattern(DyeColor.YELLOW, PatternType.HALF_VERTICAL),
            Pattern(DyeColor.ORANGE, PatternType.STRIPE_LEFT),
            Pattern(DyeColor.BLUE, PatternType.STRIPE_RIGHT),
            Pattern(DyeColor.RED, PatternType.BORDER)
        )
    ),
    TRANS_PRIDE(
        ItemStack(Material.WHITE_BANNER).putPatterns(
            Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER),
            Pattern(DyeColor.PINK, PatternType.STRIPE_SMALL),
            Pattern(DyeColor.PINK, PatternType.STRIPE_SMALL),
            Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_LEFT),
            Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_RIGHT)
        )
    ),
    HYPIXEL_STUDIOS(
        ItemStack(Material.BLUE_BANNER).putPatterns(
            Pattern(DyeColor.BLUE, PatternType.HALF_VERTICAL),
            Pattern(DyeColor.BLUE, PatternType.DIAGONAL_LEFT),
            Pattern(DyeColor.BLUE, PatternType.STRIPE_CENTER),
            Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE),
            Pattern(DyeColor.BLUE, PatternType.TRIANGLES_TOP),
            Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT),
            Pattern(DyeColor.WHITE, PatternType.STRIPE_LEFT),
            Pattern(DyeColor.BLUE, PatternType.BORDER),
            Pattern(DyeColor.BLUE, PatternType.TRIANGLES_BOTTOM)
        )
    ),
    HYTALE_INFO(
        ItemStack(Material.BLUE_BANNER).putPatterns(
            Pattern(DyeColor.WHITE, PatternType.RHOMBUS_MIDDLE),
            Pattern(DyeColor.WHITE, PatternType.TRIANGLE_BOTTOM),
            Pattern(DyeColor.RED, PatternType.CIRCLE_MIDDLE)
        )
    ),
    SCARAK(
        ItemStack(Material.WHITE_BANNER).putPatterns(
            Pattern(DyeColor.LIGHT_BLUE, PatternType.HALF_HORIZONTAL),
            Pattern(DyeColor.BROWN, PatternType.STRIPE_MIDDLE),
            Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER),
            Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP),
            Pattern(DyeColor.ORANGE, PatternType.HALF_HORIZONTAL_MIRROR),
            Pattern(DyeColor.GREEN, PatternType.SQUARE_BOTTOM_LEFT),
            Pattern(DyeColor.GREEN, PatternType.SQUARE_BOTTOM_RIGHT),
            Pattern(DyeColor.BLACK, PatternType.BORDER)
        )
    ),
    KWEEBEC(
        ItemStack(Material.BLACK_BANNER).putPatterns(
            Pattern(DyeColor.ORANGE, PatternType.STRIPE_CENTER),
            Pattern(DyeColor.ORANGE, PatternType.STRIPE_TOP),
            Pattern(DyeColor.ORANGE, PatternType.STRIPE_BOTTOM),
            Pattern(DyeColor.ORANGE, PatternType.CROSS),
            Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM),
            Pattern(DyeColor.LIME, PatternType.SQUARE_TOP_LEFT),
            Pattern(DyeColor.LIME, PatternType.TRIANGLES_TOP)
        )
    ),
    TRORK(
        ItemStack(Material.WHITE_BANNER).putPatterns(
            Pattern(DyeColor.CYAN, PatternType.STRIPE_CENTER),
            Pattern(DyeColor.CYAN, PatternType.STRIPE_TOP),
            Pattern(DyeColor.CYAN, PatternType.STRIPE_BOTTOM),
            Pattern(DyeColor.CYAN, PatternType.CROSS),
            Pattern(DyeColor.WHITE, PatternType.TRIANGLES_BOTTOM),
            Pattern(DyeColor.CYAN, PatternType.HALF_HORIZONTAL),
            Pattern(DyeColor.CYAN, PatternType.RHOMBUS_MIDDLE),
            Pattern(DyeColor.BROWN, PatternType.TRIANGLE_TOP),
            Pattern(DyeColor.BROWN, PatternType.TRIANGLES_TOP)
        )
    );

    fun getIcon(playerUuid: UUID, selected: Boolean = true): Icon {
        return Icon(getDisplayItem(playerUuid, selected)).addClickAction { player, _ ->
            player.playerData()?.bannerCosmetic = this

            if (this == NONE) {
                player.sendTranslated("system.banners.removed")
            } else {
                player.sendTranslated(
                    "system.banners.equipped",
                    Placeholder.component(
                        "banner_name",
                        playerUuid.translate("global.cosmetic.banner.${name.lowercase()}.name")
                    )
                )

                player.inventory.first { it != null && MaterialSetTag.ITEMS_BANNERS.isTagged(it.type) }
                    .apply {
                        val bannerMeta = this.itemMeta as BannerMeta
                        bannerMeta.patterns = (banner!!.itemMeta as BannerMeta).patterns
                        this.itemMeta = bannerMeta
                        this.type = banner.type
                    }
            }

            player.playSound(Sound.UI_BUTTON_CLICK)
        }
    }

    fun getDisplayItem(playerUuid: UUID, selected: Boolean = true): ItemStack {
        val isNone = this == NONE
        val language = playerUuid.playerData()?.locale ?: LanguageManager.DEFAULT_LANGUAGE

        return if (!isNone) banner!!.clone()
            .nameTranslatable("global.cosmetic.banner.${name.lowercase()}.name", language)
            .loreTranslatable(
                if (selected) "menu.banners.selectedLore" else
                    "menu.banners.selectLore", language, Placeholder.component(
                    "banner_name",
                    playerUuid.translate("global.cosmetic.banner.${name.lowercase()}.name")
                )
            )
            .flag(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ITEM_SPECIFICS)
            .enchantment(if (selected) Enchantment.ARROW_DAMAGE else null, 1)
        else ItemStack(Material.BARRIER)
            .nameTranslatable("menu.banners.deselect.name", language)
            .loreTranslatable("menu.banners.deselect.lore", language)
    }

    fun getBanner(player: Player): ItemStack? {
        return banner?.clone()?.flag(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ITEM_SPECIFICS)
            ?.nameTranslatable(
                "global.cosmetic.banner.${name.lowercase()}.name",
                player.playerData()?.locale ?: LanguageManager.DEFAULT_LANGUAGE
            )
    }

}