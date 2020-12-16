package com.comcast.telemetry.zuulelasticsearchgateway.filters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends ZuulFilter {

    @Value("${rate.limit.overdraft:50}")
    private Integer overdraft;

    @Value("${rate.limit.tokens:10}")
    private Integer tokens;

    @Value("${rate.limit.seconds:1}")
    private Integer seconds;

    @Override
    public String filterType() {
        return "pre";
    }

    /**
     * Important Note: this is always the first filter to run
     */
    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest servletRequest = requestContext.getRequest();
        HttpServletResponse servletResponse = requestContext.getResponse();
        HttpSession session = servletRequest.getSession(true);

        String appKey = servletRequest.getRemoteAddr();
        Bucket bucket = (Bucket) session.getAttribute("throttler-" + appKey);
        if (bucket == null) {
            bucket = createNewBucket();
            session.setAttribute("throttler-" + appKey, bucket);
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        // tryConsume returns false immediately if no tokens available with the bucket
        if (probe.isConsumed()) {
            // the limit is not exceeded
            servletResponse.setHeader("X-Rate-Limit-Remaining", "" + probe.getRemainingTokens());
        } else {
            // limit is exceeded
            servletResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "" + TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            String message = "The rate limiting policy has determined the request exceeds the configured threshold";
            throw new ZuulException(message, HttpStatus.TOO_MANY_REQUESTS.value(), message);
        }
        return null;
    }

    private Bucket createNewBucket() {
        Refill refill = Refill.greedy(tokens, Duration.ofSeconds(seconds));
        Bandwidth limit = Bandwidth.classic(overdraft, refill);
        return Bucket4j.builder().addLimit(limit).build();
    }
}
