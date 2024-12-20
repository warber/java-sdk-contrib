package dev.openfeature.contrib.providers.flagd;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

@Slf4j
public class MonitorChannelStateTest {

    private static final int ITERATIONS = 120000;

    @Benchmark
    @Warmup(iterations = 1)
    @Measurement(iterations = 1)
    @Test
    public void testRecursionDepth() {
        /*
        Add a breakpoint to
        dev.openfeature.contrib.providers.flagd.resolver.common.Util.monitorChannelState
        and watch the stack (not) grow
         */
        AtomicInteger conn = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();

        ManagedChannel a = mock(ManagedChannel.class);
        doNothing().when(a).notifyWhenStateChanged(any(), any());
        doNothing().when(a).notifyWhenStateChanged(any(), any());

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);


        dev.openfeature.contrib.providers.flagd.resolver.common.Util.monitorChannelState(
                ConnectivityState.SHUTDOWN,
                a,
                conn::incrementAndGet,
                error::incrementAndGet
        );

        verify(a).notifyWhenStateChanged(any(), captor.capture());

        ConnectivityState[] states = ConnectivityState.values();
        for (int i = 0; i < ITERATIONS; i++) {
            when(a.getState(anyBoolean())).thenReturn(states[i % states.length]);
            captor.getValue().run();
        }
    }
}
