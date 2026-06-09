package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.config.properties.QueueProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiredRequestSweeperTest {

    @Mock
    private RequestQueue requestQueue;

    @Mock
    private QueueProperties queueProperties;

    private ExpiredRequestSweeper sweeper;

    @BeforeEach
    void setUp() {
        sweeper = new ExpiredRequestSweeper(requestQueue, queueProperties);
    }

    @Test
    void whenQueueIsEmpty_doesNotEvict() {
        when(requestQueue.isEmpty()).thenReturn(true);

        sweeper.sweep();

        verify(requestQueue, never()).evictExpiredRequests(anyInt());
    }

    @Test
    void whenQueueIsNotEmpty_evictsWithMaxWaitMinutes() {
        when(requestQueue.isEmpty()).thenReturn(false);
        when(queueProperties.getMaxWaitMinutes()).thenReturn(5);

        sweeper.sweep();

        verify(requestQueue).evictExpiredRequests(5);
    }
}