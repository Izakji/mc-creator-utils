package com.izakji.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.izakji.agent.api.TextPart;
import com.izakji.agent.api.UserMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenEstimatorTest {

    @Test
    void estimatesTextAndAddsMessageOverhead() {
        TokenEstimator estimator = new TokenEstimator();
        int shortMessage = estimator.estimate(new UserMessage(List.of(new TextPart("hello"))));
        int longMessage = estimator.estimate(new UserMessage(List.of(new TextPart("hello world"))));
        assertTrue(longMessage > shortMessage);
        assertTrue(shortMessage >= 4);
    }
}
