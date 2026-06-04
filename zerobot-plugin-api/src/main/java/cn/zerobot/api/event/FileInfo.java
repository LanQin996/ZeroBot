package cn.zerobot.api.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OneBot 文件信息。
 * <p>
 * 可来自消息段 {@code type=file}，也可来自群文件上传通知 {@code notice_type=group_upload}。
 */
public record FileInfo(
        String id,
        String name,
        Long size,
        String url,
        String busId,
        JsonNode raw
) {
    public static FileInfo from(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return new FileInfo(
                firstText(node, "id", "file_id"),
                firstText(node, "name", "file", "file_name", "filename"),
                firstLong(node, "size", "file_size"),
                firstText(node, "url"),
                firstText(node, "busid", "bus_id"),
                node
        );
    }

    public boolean hasDownloadUrl() {
        return url != null && !url.isBlank();
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static Long firstLong(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && value.canConvertToLong()) {
                return value.asLong();
            }
        }
        return null;
    }
}
