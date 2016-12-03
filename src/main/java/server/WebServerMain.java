package server;

import io.reactivex.Flowable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: xudong
 * Date: 01/12/2016
 * Time: 11:44 AM
 */
@SpringBootApplication
public class WebServerMain {

    @Bean
    public AtomicInteger count() {
        return new AtomicInteger(0);
    }

    public static void main(String[] args) {
        final ConfigurableApplicationContext context = SpringApplication.run(WebServerMain.class);
        final AtomicInteger count = context.getBean(AtomicInteger.class);

        Flowable.interval(1, TimeUnit.SECONDS).blockingSubscribe(l ->
                System.out.println(count.get() + " requests are processing"));

    }
}
