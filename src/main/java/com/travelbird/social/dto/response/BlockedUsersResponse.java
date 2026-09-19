package com.travelbird.social.dto.response;

import java.util.List;

public record BlockedUsersResponse(
        List<BlockedUserItem> items,
        Long nextCursor
) {
}
