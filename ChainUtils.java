package com.comcast.mirs.sixoneone.utils;

import org.apache.commons.chain.Chain;
import org.apache.commons.chain.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 */
@Component
public class ChainUtils {

    private static final Logger logger = LoggerFactory.getLogger(ChainUtils.class);

    @Value("${chain.threadpool.size}")
    private Integer chainThreadPoolSize;

    private ExecutorService executor;

    @PostConstruct
    private void postConstruct() {
        executor = Executors.newFixedThreadPool(chainThreadPoolSize);
    }

    /**
     *
     * @param chain
     * @param context
     * @return
     * @throws Exception
     */
    public Future<Boolean> runChainInSeparateThread(Chain chain, Context context) throws Exception {
        return executor.submit(() -> {
            return chain.execute(context);
        });
    }

    /**
     *
     * @param chain
     * @param context
     */
    public void executeQuietly(Chain chain, Context context) {

        try {
            Boolean ignored = chain.execute(context);
        } catch (Exception ex) {
            logger.warn("Exception while running chain", ex);
        } finally {
            ;
        }
    }
}
