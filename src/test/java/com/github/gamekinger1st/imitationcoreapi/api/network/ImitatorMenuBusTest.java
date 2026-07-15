package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorMenuRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImitatorMenuBusTest {
    @Test
    void onlyForwardsConcreteMenuRequests() {
        ImitatorMenuBus bus = new ImitatorMenuBus();
        AtomicInteger requests = new AtomicInteger();
        bus.register(request -> requests.incrementAndGet());

        bus.post(ImitatorMenuRequest.NONE);
        bus.post(ImitatorMenuRequest.COMMIT_RECORD);

        assertEquals(1, requests.get());
    }
}
