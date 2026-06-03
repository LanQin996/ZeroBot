package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * NapCat/OneBot 推送事件的基础类型。
 * <p>
 * 类型化事件会封装常用字段，但 ZeroBot 仍然通过 {@link #raw()} 保留原始 JSON。
 * 当 NapCat 新增字段，或者框架暂时没有封装某个字段时，插件可以直接从原始 JSON 读取。
 */
public class OneBotEvent {
    private final JsonNode raw;

    public OneBotEvent(JsonNode raw) {
        this.raw = raw;
    }

    /**
     * NapCat 推送过来的完整原始事件 JSON。
     */
    public JsonNode raw() {
        return raw;
    }

    /**
     * OneBot 事件大类。
     * <p>
     * 常见值包括 {@code message}、{@code notice}、{@code request}、{@code meta_event}。
     */
    public String postType() {
        return text("post_type");
    }

    /**
     * 从事件根对象读取字符串字段。
     */
    public String text(String field) {
        JsonNode node = raw.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    /**
     * 从事件根对象读取 long 字段，字段不存在时返回 0。
     */
    public long longValue(String field) {
        JsonNode node = raw.get(field);
        return node == null || node.isNull() ? 0L : node.asLong();
    }
}
