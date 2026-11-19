package dev.dsmnh.horizon.adminmonitor.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;

/**
 * 物品 id 与注册名转显示名。
 *
 * <p>只做尽力解析：服务端能取到名称时用名称，取不到时原样返回调用方给出的字符串，
 * 因此解析失败不会影响播报本身。名称来自服务端语言表，未装中文语言文件时可能是英文。</p>
 */
public final class ItemNames {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<String, String>();

    private static final String[] ITEM_LOOKUP = {"func_150899_d", "getItemById"};
    private static final String[] DISPLAY_NAME = {"func_77653_i", "getItemStackDisplayName"};

    private static volatile boolean prepared;
    private static Method itemByIdMethod;
    private static Method registryLookupMethod;
    private static Constructor<?> stackConstructor;
    private static Method displayNameMethod;

    private ItemNames() {
    }

    /** 解析命令参数中的物品名或数字 id。 */
    public static String resolve(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String key = raw.trim();
        String cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String name = lookupByName(key);
        CACHE.put(key, name);
        return name;
    }

    /** 解析数字 id 与 meta，供 NEI 取物使用。 */
    public static String resolve(int id, int meta) {
        String key = id + ":" + meta;
        String cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        prepare();
        String name = displayName(itemById(id), meta);
        if (name == null || name.isEmpty()) {
            name = String.valueOf(id);
        }
        CACHE.put(key, name);
        return name;
    }

    private static String lookupByName(String raw) {
        prepare();
        Object item = null;
        if (raw.matches("\\d+")) {
            item = itemById(Integer.parseInt(raw));
        } else {
            int colon = raw.indexOf(':');
            String modId = colon > 0 ? raw.substring(0, colon) : "minecraft";
            String path = colon > 0 ? raw.substring(colon + 1) : raw;
            item = itemFromRegistry(modId, path);
            if (item == null) {
                item = itemFromMaterial(path);
            }
        }
        String name = displayName(item, 0);
        return name == null || name.isEmpty() ? raw : name;
    }

    private static Object itemFromMaterial(String path) {
        try {
            Material material = Material.matchMaterial(path.toUpperCase(Locale.ROOT));
            return material == null ? null : itemById(material.getId());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Object itemById(int id) {
        if (itemByIdMethod == null) {
            return null;
        }
        try {
            return itemByIdMethod.invoke(null, id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object itemFromRegistry(String modId, String path) {
        if (registryLookupMethod == null) {
            return null;
        }
        try {
            return registryLookupMethod.invoke(null, modId, path);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String displayName(Object item, int meta) {
        if (item == null || stackConstructor == null || displayNameMethod == null) {
            return null;
        }
        try {
            Object stack = stackConstructor.newInstance(item, 1, meta);
            Object name = displayNameMethod.invoke(item, stack);
            return name == null ? null : name.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void prepare() {
        if (prepared) {
            return;
        }
        synchronized (ItemNames.class) {
            if (prepared) {
                return;
            }
            prepared = true;
            try {
                Class<?> itemClass = Class.forName("net.minecraft.item.Item");
                itemByIdMethod = findMethod(itemClass, ITEM_LOOKUP, int.class);
                Class<?> stackClass = Class.forName("net.minecraft.item.ItemStack");
                stackConstructor = stackClass.getConstructor(itemClass, int.class, int.class);
                displayNameMethod = findMethod(itemClass, DISPLAY_NAME, stackClass);
            } catch (Throwable ignored) {
                // 反射不可用时退回原始字符串，播报内容不受影响
            }
            try {
                Class<?> registry = Class.forName("cpw.mods.fml.common.registry.GameRegistry");
                registryLookupMethod = registry.getMethod("findItem", String.class, String.class);
            } catch (Throwable ignored) {
                // 同上
            }
        }
    }

    private static Method findMethod(Class<?> owner, String[] names, Class<?>... parameters) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, parameters);
                if (method != null) {
                    return method;
                }
            } catch (Throwable ignored) {
                // 继续尝试下一个名称
            }
        }
        return null;
    }
}
