package cn.zerobot.core.napcat;

import cn.zerobot.api.ActionResponse;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

record PendingAction(
        CompletableFuture<ActionResponse<JsonNode>> future,
        ScheduledFuture<?> timeout
) {
}
