package client;

import io.reactivex.Flowable;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.AsyncRestTemplate;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: xudong
 * Date: 01/12/2016
 * Time: 12:05 PM
 */
public class AsyncRestTemplateWebClient {


    private static final int TOTAL_REQUEST = 10000;

    private static final int MAX_CONCURRENCY = 1000;

    public static final int TIMEOUT = 20 * 1000;

    private static AtomicInteger runningCount = new AtomicInteger(0);

    private static AtomicInteger successCount = new AtomicInteger(0);

    private static AtomicInteger failureCount = new AtomicInteger(0);

    private AsyncRestTemplate restTemplate;

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 1) {
            printHelp();
        }

        Thread.sleep(5000l);

        AsyncRestTemplate asyncRestTemplate = null;

        switch (args[0]) {
            case "Default":
                asyncRestTemplate = defaultAsyncRestTemplate();
                break;

            case "CloseableHttpAsyncClient":
                asyncRestTemplate = closeableHttpAsyncClientAsyncRestTemplate();
                break;

            case "OkHttp3":
                asyncRestTemplate = okHttp3AsyncRestTemplate();
                break;

            case "Netty4":
                asyncRestTemplate = netty4RestTemplate();
                break;

            default:
                printHelp();
                return;
        }

        AsyncRestTemplateWebClient client = new AsyncRestTemplateWebClient(asyncRestTemplate);

        long start = System.currentTimeMillis();

        Flowable.interval(1, TimeUnit.SECONDS).subscribe(l ->
                System.out.println(String.format("concurrent running %d, success %d, fail %d",
                        runningCount.get(), successCount.get(), failureCount.get())));

        Flowable.range(1, TOTAL_REQUEST)
                .flatMap(i -> Flowable.defer(() -> toFlowable(client.asyncAccessRemote(i))), MAX_CONCURRENCY)
                .blockingSubscribe(s -> System.out.println(s));

        long end = System.currentTimeMillis();
        System.out.println(
                String.format("%d requests, %d concurrency, consume %d", TOTAL_REQUEST, MAX_CONCURRENCY,
                        (end - start)));

    }

    private static void printHelp() {
        System.out.println("please specify implementation: [Default|CloseableHttpAsyncClient|OkHttp3|Netty4]");
    }

    private static AsyncRestTemplate netty4RestTemplate() {

        Netty4ClientHttpRequestFactory netty4ClientHttpRequestFactory = new Netty4ClientHttpRequestFactory();
        netty4ClientHttpRequestFactory.setReadTimeout(TIMEOUT);
        netty4ClientHttpRequestFactory.setConnectTimeout(TIMEOUT);

        AsyncRestTemplate restTemplate = new AsyncRestTemplate();
        restTemplate.setAsyncRequestFactory(netty4ClientHttpRequestFactory);

        return restTemplate;

    }

    private static AsyncRestTemplate okHttp3AsyncRestTemplate() {
        final Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(MAX_CONCURRENCY);
        dispatcher.setMaxRequestsPerHost(MAX_CONCURRENCY);

        OkHttpClient client = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .retryOnConnectionFailure(true)
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        OkHttp3ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory(client);

        AsyncRestTemplate restTemplate = new AsyncRestTemplate();

        restTemplate.setAsyncRequestFactory(requestFactory);

        return restTemplate;
    }

    private static AsyncRestTemplate closeableHttpAsyncClientAsyncRestTemplate() {

        IOReactorConfig reactorConfig = IOReactorConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setSoTimeout(TIMEOUT)
                .build();

        HttpAsyncClientBuilder asyncClientBuilder = HttpAsyncClientBuilder.create();
        asyncClientBuilder.setDefaultIOReactorConfig(reactorConfig);
        asyncClientBuilder.setMaxConnPerRoute(MAX_CONCURRENCY).setMaxConnTotal(MAX_CONCURRENCY);

        final CloseableHttpAsyncClient httpAsyncClient = asyncClientBuilder.build();

        AsyncRestTemplate restTemplate = new AsyncRestTemplate();
        restTemplate.setAsyncRequestFactory(new HttpComponentsAsyncClientHttpRequestFactory(httpAsyncClient));

        return restTemplate;

    }

    private static AsyncRestTemplate defaultAsyncRestTemplate() {


        final SimpleClientHttpRequestFactory asyncRequestFactory = new SimpleClientHttpRequestFactory();
        asyncRequestFactory.setConnectTimeout(TIMEOUT);
        asyncRequestFactory.setReadTimeout(TIMEOUT);
        final SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        asyncRequestFactory.setTaskExecutor(taskExecutor);

        AsyncRestTemplate restTemplate = new AsyncRestTemplate();
        restTemplate.setAsyncRequestFactory(asyncRequestFactory);

        return restTemplate;
    }


    public AsyncRestTemplateWebClient(AsyncRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private CompletableFuture<String> asyncAccessRemote(int i) {

        CompletableFuture<String> promise = new CompletableFuture<>();

        runningCount.incrementAndGet();

        restTemplate.getForEntity("http://127.0.0.1:8080/slow?i=" + i, String.class).addCallback(
                result -> promise.complete(result.getBody()),
                ex -> promise.completeExceptionally(ex));

        return promise;
    }

    private static Flowable<String> toFlowable(CompletableFuture<String> future) {
        return Flowable.<String>defer(() -> emitter ->
                future.whenComplete((result, error) -> {
                    runningCount.decrementAndGet();

                    if (error != null) {
                        failureCount.incrementAndGet();
                        emitter.onError(error);
                    } else {
                        successCount.incrementAndGet();
                        emitter.onNext(result);
                        emitter.onComplete();
                    }
                })).onExceptionResumeNext(Flowable.just("oops"));
    }

}
