package server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: xudong
 * Date: 01/12/2016
 * Time: 11:44 AM
 */
@Controller
public class WebSlowController {
    @Autowired
    private AtomicInteger totalCount;

    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * a slow operation that will take 10s
     * @param i
     * @return
     * @throws InterruptedException
     */
    @RequestMapping(path = "/slow")
    @ResponseBody
    public CompletableFuture slow(@RequestParam("i") int i) throws InterruptedException {

        totalCount.incrementAndGet();

        CompletableFuture future = new CompletableFuture();
        scheduledExecutorService.schedule(() -> {
            totalCount.decrementAndGet();
            future.complete(String.valueOf(i + 1000000));
        }, 10, TimeUnit.SECONDS);

        return future;
    }
}
