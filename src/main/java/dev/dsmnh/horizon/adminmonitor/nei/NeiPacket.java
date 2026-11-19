package dev.dsmnh.horizon.adminmonitor.nei;

import java.nio.ByteBuffer;

/**
 * NEI 作弊面板下发到服务端的动作包。
 *
 * <p>动作号取自 NEI 的 {@code PacketIDs.C2S}，字段顺序取自同版本 {@code NEISPH} 与
 * {@code PacketCustom}：首字节为动作号，其后按动作排列。压缩包的标志位在首字节的最高位，
 * 本类只处理未压缩的包。</p>
 */
public final class NeiPacket {

    public static final int GIVE_ITEM = 1;
    public static final int SET_TIME = 7;
    public static final int HEAL = 8;
    public static final int TOGGLE_RAIN = 9;
    public static final int SET_GAME_MODE = 13;
    public static final int MODIFY_ENCHANTMENT = 22;

    private final int type;
    private final int itemId;
    private final int count;
    private final int meta;
    private final int value;
    private final int extra;

    private NeiPacket(int type, int itemId, int count, int meta, int value, int extra) {
        this.type = type;
        this.itemId = itemId;
        this.count = count;
        this.meta = meta;
        this.value = value;
        this.extra = extra;
    }

    /** 解析负载；不属于本插件处理范围或长度不足时返回 {@code null}。 */
    public static NeiPacket parse(byte[] data) {
        if (data == null || data.length < 2) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int type = buffer.get() & 0xFF;
        try {
            switch (type) {
                case GIVE_ITEM:
                    return readGiveItem(buffer);
                case SET_TIME:
                case SET_GAME_MODE:
                    return new NeiPacket(type, 0, 0, 0, buffer.get() & 0xFF, 0);
                case HEAL:
                case TOGGLE_RAIN:
                    return new NeiPacket(type, 0, 0, 0, 0, 0);
                case MODIFY_ENCHANTMENT:
                    return new NeiPacket(type, 0, 0, 0, buffer.get() & 0xFF, buffer.get() & 0xFF);
                default:
                    return null;
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static NeiPacket readGiveItem(ByteBuffer buffer) {
        int itemId = buffer.getInt();
        int count = buffer.getInt();
        int meta = buffer.getShort() & 0xFFFF;
        // FML 的 ByteBufUtils.writeTag 对无 NBT 的物品写 -1，不能按无符号读
        int nbtLength = buffer.getShort();
        if (nbtLength > 0 && buffer.remaining() >= nbtLength) {
            buffer.position(buffer.position() + nbtLength);
        }
        return new NeiPacket(GIVE_ITEM, itemId, count, meta, 0, 0);
    }

    public int type() {
        return type;
    }

    public int itemId() {
        return itemId;
    }

    public int count() {
        return count;
    }

    public int meta() {
        return meta;
    }

    /** 改时间时为小时数，改游戏模式时为模式编号，改附魔时为附魔编号。 */
    public int value() {
        return value;
    }

    /** 改附魔时的等级，其余动作为 0。 */
    public int extra() {
        return extra;
    }

    /** NEI 鉴权使用的动作名，用于确认该动作会被服务端执行。 */
    public String action() {
        switch (type) {
            case GIVE_ITEM:
                return "item";
            case SET_TIME:
                return "time";
            case HEAL:
                return "heal";
            case TOGGLE_RAIN:
                return "rain";
            case SET_GAME_MODE:
                return gameModeAction(value);
            case MODIFY_ENCHANTMENT:
                return "enchant";
            default:
                return "";
        }
    }

    private static String gameModeAction(int mode) {
        switch (mode) {
            case 1:
                return "creative";
            case 2:
                return "creative+";
            case 3:
                return "adventure";
            default:
                return "";
        }
    }
}
