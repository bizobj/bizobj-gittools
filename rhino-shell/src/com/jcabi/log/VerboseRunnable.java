/**
 * Copyright (c) 2012-2013, JCabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.log;

import java.util.concurrent.Callable;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Wrapper of {@link Runnable}, that logs all uncaught runtime exceptions.
 *
 * <p>You can use it with scheduled executor, for example:
 *
 * <pre> Executors.newScheduledThreadPool(2).scheduleAtFixedRate(
 *   new VerboseRunnable(runnable, true), 1L, 1L, TimeUnit.SECONDS
 * );</pre>
 *
 * <p>Now, every runtime exception that is not caught inside your
 * {@link Runnable} will be reported to log (using {@link Logger}).
 * Two-arguments constructor can be used when you need to instruct the class
 * about what to do with the exception: either swallow it or escalate.
 * Sometimes it's very important to swallow exceptions. Otherwise an entire
 * thread may get stuck (like in the example above).
 *
 * <p>Since version 0.7.16, besides just swallowing exceptions
 * the class also clears the interrupted status. That means that the thread
 * with the runnable can be interrupted but will never expose its interrupted
 * status.
 *
 * <p>This class is thread-safe.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.1.3
 * @see VerboseThreads
 * @link <a href="http://www.ibm.com/developerworks/java/library/j-jtp05236/index.html">Java theory and practice: Dealing with InterruptedException</a>
 */
@ToString
@EqualsAndHashCode(of = { "origin", "swallow" })
@SuppressWarnings("PMD.DoNotUseThreads")
public final class VerboseRunnable implements Runnable {

    /**
     * Original runnable.
     */
    private final transient Runnable origin;

    /**
     * Swallow exceptions?
     */
    private final transient boolean swallow;

    /**
     * Shall we report a full stacktrace?
     */
    private final transient boolean verbose;

    /**
     * Default constructor, doesn't swallow exceptions.
     * @param runnable Runnable to wrap
     */
    public VerboseRunnable(@NotNull final Runnable runnable) {
        this(runnable, false);
    }

    /**
     * Default constructor, doesn't swallow exceptions.
     * @param callable Callable to wrap
     * @since 0.7.17
     */
    public VerboseRunnable(@NotNull final Callable<?> callable) {
        this(callable, false);
    }

    /**
     * Default constructor, doesn't swallow exceptions.
     * @param callable Callable to wrap
     * @param swallowexceptions Shall we swallow exceptions
     *  ({@code TRUE}) or re-throw
     *  ({@code FALSE})? Exception swallowing means that {@link #run()}
     *  will never throw any exceptions (in any case all exceptions are logged
     *  using {@link Logger}.
     * @since 0.1.10
     */
    public VerboseRunnable(@NotNull final Callable<?> callable,
        final boolean swallowexceptions) {
        this(callable, swallowexceptions, true);
    }

    /**
     * Default constructor.
     * @param callable Callable to wrap
     * @param swallowexceptions Shall we swallow exceptions
     *  ({@code TRUE}) or re-throw
     *  ({@code FALSE})? Exception swallowing means that {@link #run()}
     *  will never throw any exceptions (in any case all exceptions are logged
     *  using {@link Logger}.
     * @param fullstacktrace Shall we report the entire
     *  stacktrace of the exception
     *  ({@code TRUE}) or just its message in one line ({@code FALSE})
     * @since 0.7.17
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public VerboseRunnable(@NotNull final Callable<?> callable,
        final boolean swallowexceptions, final boolean fullstacktrace) {
        this(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        callable.call();
                    // @checkstyle IllegalCatch (1 line)
                    } catch (Exception ex) {
                        throw new IllegalArgumentException(ex);
                    }
                }
                @Override
                public String toString() {
                    return callable.toString();
                }
            },
            swallowexceptions,
            fullstacktrace
        );
    }

    /**
     * Default constructor, with configurable behavior for exceptions.
     * @param runnable Runnable to wrap
     * @param swallowexceptions Shall we swallow exceptions
     *  ({@code TRUE}) or re-throw
     *  ({@code FALSE})? Exception swallowing means that {@link #run()}
     *  will never throw any exceptions (in any case all exceptions are logged
     *  using {@link Logger}.
     * @since 0.1.4
     */
    public VerboseRunnable(@NotNull final Runnable runnable,
        final boolean swallowexceptions) {
        this(runnable, swallowexceptions, true);
    }

    /**
     * Default constructor, with fully configurable behavior.
     * @param runnable Runnable to wrap
     * @param swallowexceptions Shall we swallow exceptions
     *  ({@code TRUE}) or re-throw
     *  ({@code FALSE})? Exception swallowing means that {@link #run()}
     *  will never throw any exceptions (in any case all exceptions are logged
     *  using {@link Logger}.
     * @param fullstacktrace Shall we report the entire
     *  stacktrace of the exception
     *  ({@code TRUE}) or just its message in one line ({@code FALSE})
     * @since 0.7.17
     */
    public VerboseRunnable(@NotNull final Runnable runnable,
        final boolean swallowexceptions, final boolean fullstacktrace) {
        this.origin = runnable;
        this.swallow = swallowexceptions;
        this.verbose = fullstacktrace;
    }

    /**
     * {@inheritDoc}
     *
     * <p>We catch {@link RuntimeException} and {@link Error} here. All other
     * types of exceptions are "checked exceptions" and won't be thrown out
     * of {@code Runnable#run()} method.
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void run() {
        try {
            this.origin.run();
        // @checkstyle IllegalCatch (1 line)
        } catch (RuntimeException ex) {
            if (!this.swallow) {
                Logger.warn(this, "escalated exception: %s", this.tail(ex));
                throw ex;
            }
            Logger.warn(this, "swallowed exception: %s", this.tail(ex));
        // @checkstyle IllegalCatch (1 line)
        } catch (Error error) {
            if (!this.swallow) {
                Logger.error(this, "escalated error: %s", this.tail(error));
                throw error;
            }
            Logger.error(this, "swallowed error: %s", this.tail(error));
        }
        if (this.swallow) {
            try {
                Thread.sleep(1L);
            } catch (InterruptedException ex) {
                Logger.debug(
                    this,
                    "interrupted status cleared of %s: %s",
                    Thread.currentThread().getName(), ex
                );
            }
        }
    }

    /**
     * Make a tail of the error/warning message, using the exception thrown.
     * @param throwable The exception/error caught
     * @return The message to show in logs
     */
    private String tail(final Throwable throwable) {
        final String tail;
        if (this.verbose) {
            tail = Logger.format("%[exception]s", throwable);
        } else {
            tail = Logger.format(
                "%[type]s('%s')",
                throwable,
                throwable.getMessage()
            );
        }
        return tail;
    }

}
