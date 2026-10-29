package ma.aui.csc.monolith.healthlink.common.data;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PageRequests {
    private PageRequests() {}

    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be >= 0; size must be between 1 and 100");
        }
        return PageRequest.of(page, size, sort);
    }
}
