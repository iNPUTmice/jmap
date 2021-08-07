/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2016 Christian Schudt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package rs.ltt.jmap.client.event;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This is the default reconnection strategy used.
 *
 * <p>It exponentially increases the time span from which a random value for the next reconnection attempt is chosen.</p>
 *
 * <p>The formula for doing this, is: <code>(2<sup>n</sup> - 1) * s</code>, where <code>n</code> is the number of reconnection attempt and <code>s</code> is the slot time, which is 60 seconds by default.</p>
 *
 * <p>In practice this means, the first reconnection attempt occurs after a random period of time between 0 and 60 seconds.<br>
 * The second attempt chooses a random number &gt;= 0 and &lt; 180 seconds.<br>
 * The third attempt chooses a random number &gt;= 0 and &lt; 420 seconds.<br>
 * The fourth attempt chooses a random number &gt;= 0 and &lt; 900 seconds.<br>
 * The fifth attempt chooses a random number &gt;= 0 and &lt; 1860 seconds (= 31 minutes)<br>
 * </p>
 *
 * <p>The strategy is called "truncated", because it won't increase the time span after the nth iteration, which means in the example above, the sixth and any further attempt
 * behaves equally to the fifth attempt.</p>
 *
 * <p>This "truncated binary exponential backoff" is the <a href="https://xmpp.org/rfcs/rfc6120.html#tcp-reconnect">recommended reconnection strategy by the XMPP specification</a>.</p>
 *
 * <p>This class is immutable.</p>
 *
 * @author Christian Schudt
 */
class TruncatedBinaryExponentialBackoffStrategy implements ReconnectionStrategy {

    private final int slotTime;

    private final int ceiling;

    /**
     * @param slotTime The slot time (in seconds), usually 60.
     * @param ceiling  The ceiling, i.e. when the time is truncated. E.g. if the ceiling is 4, the back off is truncated at the 5th reconnection attempt (it starts at zero).
     */
    TruncatedBinaryExponentialBackoffStrategy(int slotTime, int ceiling) {
        this.slotTime = slotTime;
        this.ceiling = ceiling;
    }

    @Override
    public Duration getNextReconnectionAttempt(int attempt) {
        // For the first attempt choose a random number between 0 and 60.
        // For the second attempt choose a random number between 0 and 180.
        // For the third attempt choose a random number between 0 and 420.
        // For the fourth attempt choose a random number between 0 and 900.
        // For the fifth attempt choose a random number between 0 and 1860.
        // ==> max wait time: 1860 seconds = 31 minutes. (if ceiling == 5)
        return Duration.ofSeconds(ThreadLocalRandom.current().nextInt((int) ((Math.pow(2, Math.min(attempt, ceiling) + 1) - 1) * slotTime)));
    }
}
