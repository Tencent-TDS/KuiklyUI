// Host regression test for the OpenHarmony context-worker thread wrapper.

#include "libohos_render/foundation/thread/KRSizedThread.h"

#include <pthread.h>

#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <cstdlib>
#include <mutex>
#include <string>
#include <system_error>

namespace {

struct ParkState {
    std::mutex mutex;
    std::condition_variable condition;
    bool ready{false};
    bool stop{false};
};

void Park(ParkState *state) {
    std::unique_lock<std::mutex> lock(state->mutex);
    state->ready = true;
    state->condition.notify_all();
    state->condition.wait(lock, [state]() { return state->stop; });
}

void WaitUntilReady(ParkState *state) {
    std::unique_lock<std::mutex> lock(state->mutex);
    state->condition.wait(lock, [state]() { return state->ready; });
}

void Stop(ParkState *state) {
    {
        std::lock_guard<std::mutex> lock(state->mutex);
        state->stop = true;
    }
    state->condition.notify_all();
}

std::size_t ReadStackSize(pthread_t thread) {
#if defined(__APPLE__)
    return pthread_get_stacksize_np(thread);
#elif defined(__linux__)
    pthread_attr_t attributes;
    const int getAttributeError = pthread_getattr_np(thread, &attributes);
    if (getAttributeError != 0) {
        throw std::system_error(getAttributeError, std::generic_category(), "pthread_getattr_np");
    }
    std::size_t stackSize = 0;
    const int getSizeError = pthread_attr_getstacksize(&attributes, &stackSize);
    static_cast<void>(pthread_attr_destroy(&attributes));
    if (getSizeError != 0) {
        throw std::system_error(getSizeError, std::generic_category(), "pthread_attr_getstacksize");
    }
    return stackSize;
#else
#error "The KRThread host regression test supports Linux and macOS."
#endif
}

bool TestStackSize() {
    ParkState state;
    KRSizedThread worker([&state]() { Park(&state); });
    WaitUntilReady(&state);
    const std::size_t stackSize = ReadStackSize(worker.native_handle());
    Stop(&state);
    worker.join();

    std::printf("context worker stack: %zu bytes\n", stackSize);
    return stackSize >= kKRContextWorkerStackSize && !worker.joinable();
}

struct SelfJoinState {
    std::mutex mutex;
    std::condition_variable condition;
    KRSizedThread *worker{nullptr};
    bool start{false};
    bool finished{false};
    bool rejectedSelfJoin{false};
    bool reportedDeadlock{false};
    bool retainedOwnership{false};
};

bool TestFailedJoinRetainsOwnership() {
    SelfJoinState state;
    KRSizedThread worker([&state]() {
        KRSizedThread *self = nullptr;
        {
            std::unique_lock<std::mutex> lock(state.mutex);
            state.condition.wait(lock, [&state]() { return state.start; });
            self = state.worker;
        }

        try {
            self->join();
        } catch (const std::system_error &error) {
            state.rejectedSelfJoin = true;
            state.reportedDeadlock =
                error.code() == std::make_error_code(std::errc::resource_deadlock_would_occur);
            state.retainedOwnership = self->joinable();
        }

        {
            std::lock_guard<std::mutex> lock(state.mutex);
            state.finished = true;
        }
        state.condition.notify_all();
    });

    {
        std::lock_guard<std::mutex> lock(state.mutex);
        state.worker = &worker;
        state.start = true;
    }
    state.condition.notify_all();

    {
        std::unique_lock<std::mutex> lock(state.mutex);
        const bool finished = state.condition.wait_for(
            lock, std::chrono::seconds(5), [&state]() { return state.finished; });
        if (!finished) {
            std::fprintf(stderr, "FAIL: self-join did not fail within 5 seconds\n");
            // A worker blocked in pthread_join cannot be safely joined or
            // destroyed. End only this test process so the regression remains
            // bounded and the operating system reclaims the test thread.
            std::_Exit(EXIT_FAILURE);
        }
    }

    const bool failureStateWasCorrect =
        state.rejectedSelfJoin && state.reportedDeadlock && state.retainedOwnership && worker.joinable();
    worker.join();
    return failureStateWasCorrect && !worker.joinable();
}

}  // namespace

int main() {
    if (!TestStackSize()) {
        std::fprintf(stderr, "FAIL: context worker stack is smaller than 8 MiB\n");
        return EXIT_FAILURE;
    }
    if (!TestFailedJoinRetainsOwnership()) {
        std::fprintf(stderr, "FAIL: failed join discarded thread ownership\n");
        return EXIT_FAILURE;
    }

    std::printf("PASS: stack size and join ownership semantics\n");
    return EXIT_SUCCESS;
}
